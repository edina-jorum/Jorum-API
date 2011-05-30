/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 *
 *
 *  University Of Edinburgh (EDINA) 
 *  Scotland
 *
 *
 *  File Name           : ResourceController.groovy
 *  Author              : ianfieldhouse
 *  Approver            : Ian Fieldhouse 
 * 
 *  Notes               :
 *
 *
 *~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 *
 */

package uk.ac.ed.jorum.api

import com.boxysystems.jgoogleanalytics.*
import org.apache.commons.validator.EmailValidator
import grails.converters.*
import grails.util.JSonBuilder
import groovy.xml.MarkupBuilder
import java.io.IOException
import java.io.FileInputStream
import java.io.InputStream
import java.lang.NullPointerException
import java.lang.NumberFormatException
import java.lang.String
import java.util.Properties

class ResourceController {
    def repositoryService
    private def appName     = grailsApplication.config.grails.app.name
    private def appVersion  = grailsApplication.config.grails.app.context
    private def serverUrl   = grailsApplication.config.grails.serverURL + grailsApplication.config.grails.app.context
    private def dspaceUrl   = grailsApplication.config.dspace.url
    private def analyticsId = grailsApplication.config.google.analytics.webPropertyID

    private JGoogleAnalyticsTracker tracker = new JGoogleAnalyticsTracker(appName,appVersion,analyticsId)
    
    def index = {
        renderResources(params)
    }
    
    def collection = {
        renderResources(params)
    }
    
    def author = {
        renderResources(params)
    }
    
    def keyword = {
        renderResources(params)
    }
    
    def user = {
        if (params.id != null)
        {
            String submitter_email = params.id.trim()
            EmailValidator evalidator = new EmailValidator()

            if (evalidator.isValid(submitter_email))
            {
                try 
                {
                    RepositorySearchResults results = repositoryService.findResourcesBySubmitter(submitter_email.decodeURL())
                    formatResponse(results)
                } 
                catch (NullPointerException e)
                {
                    render "Sorry, the email address supplied doesn't match any of our users."
                }
            } 
            else
            {
                render "Sorry, you haven't supplied a valid email address."
            }   
        }
        else
        {
            render "Sorry, the url you have requested is not correctly formed."
        }
    }
    
    def search = {
        
        if (params.id != null){
            def default_limit = grailsApplication.config.results.default
            int results_limit
            
            (params.limit != null) ? (results_limit = params.limit.toInteger()) : (results_limit = default_limit)
            
            String search_term = params.id.trim().encodeAsURL()
            try {
                RepositorySearchResults results = repositoryService.findResourcesBySearchTerm(search_term, results_limit)
                formatResponse(results)
            } catch (NullPointerException e){
                render "Sorry, your request didn't return any results."
            }
        }
        else
            render "Sorry, the url you have requested is not correctly formed."
    }
    
    def renderResources(params){
        def id = ""
        if (params.id != null) id = params.id.trim()
        int results_limit
        int default_limit = grailsApplication.config.results.default
        String order_direction
        String start_string
        int offset = 0
        
        (params.limit != null) ? (results_limit = params.limit.toInteger()) : (results_limit = default_limit)
        (params.order != null) ? (order_direction = params.order) : (order_direction = null)
        (params.starts_with != null) ? (start_string = params.starts_with) : (start_string = null)
        (params.offset != null) ? (offset = params.offset.toInteger()) : (offset = 0)
        
        try {
            RepositorySearchResults results = repositoryService.browseResources(actionName, id, results_limit, order_direction, start_string, offset)
            formatResponse(results)
        } catch (NullPointerException e){
            render "Sorry, this service is currently unavailable."
        }
    } 
    
    def formatResponse(results) {
        FocusPoint focusPoint
        
        withFormat {
            xml{ 
                def xml
                if (params.dc == "true"){
                    if (params.id == null)
                        focusPoint = new FocusPoint("${controllerName}/${actionName}.xml (dc)");
                    else
                        focusPoint = new FocusPoint("${controllerName}/${actionName}/" + params.id + ".xml (dc)");
                    tracker.trackAsynchronously(focusPoint);
                    xml = toDC(results)
                } else {
                    if (params.id == null)
                        focusPoint = new FocusPoint("${controllerName}/${actionName}.xml");
                    else
                        focusPoint = new FocusPoint("${controllerName}/${actionName}/" + params.id + ".xml");
                    tracker.trackAsynchronously(focusPoint);
                    xml = toXML(results)
                }
                render(text:xml, contentType:"application/xml", encoding:"UTF-8")
            }
            json{
                if (params.id == null)
                    focusPoint = new FocusPoint("${controllerName}/${actionName}.json");
                else
                    focusPoint = new FocusPoint("${controllerName}/${actionName}/" + params.id + ".json");
                tracker.trackAsynchronously(focusPoint);
                def json = toJSON(results)
                render(text:json, contentType:"application/json", encoding:"UTF-8")
            }
            jsonp{
                if (params.callback != null){
                    if (params.id == null)
                        focusPoint = new FocusPoint("${controllerName}/${actionName}.jsonp");
                    else
                        focusPoint = new FocusPoint("${controllerName}/${actionName}/" + params.id + ".jsonp");
                    tracker.trackAsynchronously(focusPoint);
                    def json = toJSON(results)
                    render(text:"${params.callback}(${json})", contentType:"application/javascript", encoding:"UTF-8")
                } else {
                    render "Please supply a callback parameter (e.g. callback=myFunc) in your request to use the JSONP service."
                }
            }
            rss{
                if (params.id == null)
                    focusPoint = new FocusPoint("${controllerName}/${actionName}.rss");
                else
                    focusPoint = new FocusPoint("${controllerName}/${actionName}/" + params.id + ".rss");
                tracker.trackAsynchronously(focusPoint);
                render(feedType:"rss", feedVersion:"2.0") {
                    title = "Resources Feed"
                    link = "${serverUrl}/${controllerName}/${actionName}/" + params.id + ".rss"
                    description = "RSS feed for resources in a DSpace ${actionName}"
                    results.result_list.each() { resource ->
                        entry(resource.title.toString()) {
                            link = "${dspaceUrl}/handle/${resource.url}"
                            resource.description
                        }
                    }
                }
            }
        }
    }
    
    private def toXML = { results -> 
        def writer = new StringWriter()
        def xml    = new MarkupBuilder(writer)
        def resource_list = results.result_list
        xml.resources{
            if (results.total != 0){
                total(results.total)
                limit(results.limit)
                sort(results.order)
                offset(results.offset)
                next_offset(results.nextOffset)
                previous_offset(results.previousOffset)
            }
            for(r in resource_list){
                resource_record{
                    id(r.id.toString())
                    title(r.title)
                    description(r.description)
                    url(dspaceUrl + '/handle/' + r.url)
                    authors{
                        if (r.authors instanceof String)
                        /* Need to prepend this method call with 'xml.' so that it 
                           doesn't  clash with the 'author' controller method otherwise  
                           it causes a StackOverflow Exception and crashes the server */
                        xml.author(r.authors)
                        else
                        r.authors.each(){ xml.author(it) }
                    }
                    keywords{
                        if (r.keywords instanceof String)
                        /* Need to prepend this method call with 'xml.' so that it 
                           doesn't  clash with the 'keyword' controller method otherwise  
                           it causes a StackOverflow Exception and crashes the server */
                        xml.keyword(r.keywords)
                        else
                        r.keywords.each(){ xml.keyword(it) }
                    }
                    rights{
                        description(r.rights_desc)
                        url(r.rights_url)
                    }
                }
            }        
        }
        return writer.toString()
    }
    
    private def toDC = { results -> 
        def writer = new StringWriter()
        def xml    = new MarkupBuilder(writer)
        def resource_list = results.result_list
        xml.'j:resources'(
            'xmlns:j': 'http://www.jorum.ac.uk/',
            'xmlns:dc': 'http://purl.org/dc/elements/1.1/',
            'xmlns:dcterms': 'http://purl.org/dc/terms/',
            'xmlns:xsi': 'http://www.w3.org/2001/XMLSchema-instance',
            'xsi:schemaLocation': "http://www.jorum.ac.uk/ ${serverUrl}/schemas/resources.xsd")
        {
            if (results.total != 0){
                total(results.total)
                limit(results.limit)
                sort(results.order)
                offset(results.offset)
                next_offset(results.nextOffset)
                previous_offset(results.previousOffset)
            }
            for(r in resource_list){
                resource_record{
                    id(r.id.toString())
                    'dc:title'(r.title)
                    'dc:description'(r.description)
                    'dc:identifier'('xsi:type': 'dcterms:URI', dspaceUrl + '/handle/' + r.url)
                    if (r.authors instanceof String)
                        'dc:contributor'(r.authors)
                    else
                        r.authors.each(){ 'dc:contributor'(it) }
                    if (r.keywords instanceof String)
                        'dc:subject'(r.keywords)
                    else
                        r.keywords.each(){ 'dc:subject'(it) }
                    'dc:rights'(r.rights_desc)
                    'dc:rights'('xsi:type': 'dcterms:URI', r.rights_url)
                }
            }        
        }
        return writer.toString()
    }
    
    private def toJSON = { 
        def resource_list = []
        it.result_list.each { resource ->
            def resource_map = [:]
            resource_map.put("id", resource.id.toString())
            resource_map.put("title", resource.title)
            resource_map.put("description", resource.description)
            resource_map.put("url", dspaceUrl + '/handle/' + resource.url)

            // Add keywords
            def keywords = []
            if (resource.keywords instanceof String){
                def keyword_map = [:]
                keyword_map.put("keyword", resource.keywords)
                keywords.add(keyword_map)
            } else {
                resource.keywords.each {
                    def keyword_map = [:]
                    keyword_map.put("keyword", it)
                    keywords.add(keyword_map)
                }
            }
            resource_map.put("keywords", keywords)

            // Add authors
            def authors = []
            if (resource.authors instanceof String){
                def author_map = [:]
                author_map.put("author", resource.authors)
                authors.add(author_map)
            } else {
                resource.authors.each { 
                    def author_map = [:]
                    author_map.put("author", it)
                    authors.add(author_map)
                }
            }
            resource_map.put("authors", authors)

            // Add rights
            def rights = []
            def rights_map = [:]
            rights_map.put("rights_url", resource.rights_url)
            rights_map.put("rights_desc", resource.rights_desc)
            rights.add(rights_map)
            resource_map.put("rights", rights)

            resource_list.add(resource_map)
        }
        
        if (it.total != 0){
            // Add browse info
            def info_map = [:]
            info_map.put("total", it.total)
            info_map.put("limit", it.limit)
            info_map.put("order", it.order)
            info_map.put("offset", it.offset)
            info_map.put("next_offset", it.nextOffset)
            info_map.put("previous_offset", it.previousOffset)
            resource_list.add(info_map)
        }
        
        def resources = [ resources: resource_list ]
        
        return resources as JSON
    }
}
