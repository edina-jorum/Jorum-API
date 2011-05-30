/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 *
 *
 *  University Of Edinburgh (EDINA) 
 *  Scotland
 *
 *
 *  File Name           : KeywordController.groovy
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
import grails.util.JSonBuilder
import groovy.xml.MarkupBuilder
import java.lang.NullPointerException

class KeywordController {
    def repositoryService
    private def appName     = grailsApplication.config.grails.app.name
    private def appVersion  = grailsApplication.config.grails.app.context
    private def analyticsId = grailsApplication.config.google.analytics.webPropertyID
    
    private JGoogleAnalyticsTracker tracker = new JGoogleAnalyticsTracker(appName,appVersion,analyticsId)
    
    def index = {
        redirect(action: "list")
    }
    
    def list = {
        def default_limit = grailsApplication.config.results.default
        int results_limit
        String order_direction
        String start_letter
        int offset
        
        (params.limit != null) ? (results_limit = params.limit.toInteger()) : (results_limit = default_limit)
        (params.order != null) ? (order_direction = params.order) : (order_direction = null)
        (params.starts_with != null) ? (start_letter = params.starts_with) : (start_letter = null)
        (params.offset != null) ? (offset = params.offset.toInteger()) : (offset = 0)
        try {
            RepositorySearchResults results = repositoryService.listKeywords(results_limit, order_direction, start_letter, offset)
            FocusPoint focusPoint
            withFormat {
                xml{ 
                    focusPoint = new FocusPoint("${controllerName}/${actionName}/" + params.id + ".xml");
                    tracker.trackAsynchronously(focusPoint);
                    def xml = toXML(results)
                    render(text:xml, contentType:"application/xml", encoding:"UTF-8")
                }
                json{
                    focusPoint = new FocusPoint("${controllerName}/${actionName}/" + params.id + ".json");
                    tracker.trackAsynchronously(focusPoint);
                    def json = toJSON(results)
                    render(text:json, contentType:"application/json", encoding:"UTF-8")
                }
                jsonp{
                    if (params.callback != null){
                        focusPoint = new FocusPoint("${controllerName}/${actionName}/" + params.id + ".jsonp");
                        tracker.trackAsynchronously(focusPoint);
                        def json = toJSON(results)
                        render(text:"${params.callback}(${json})", contentType:"application/javascript", encoding:"UTF-8")
                    } else {
                        render "Please supply a callback parameter (e.g. callback=myFunc) in your request to use the JSONP service."
                    }
                }
                rss{
                     render "The keyword list cannot be rendered as an RSS feed."
                }
            }
        } catch (NullPointerException e){
            render "Sorry, this service is currently unavailable."
        }
    }
    
    private def toXML = { keyword_list ->
        def writer = new StringWriter()
        def xml    = new MarkupBuilder(writer)
        xml.keywords{
            for (k in keyword_list.result_list) {
                keyword(k.value)
            }        
        }
        return writer.toString()
    }
    
    private def toJSON = { keyword_list ->
        def writer = new StringWriter();
        new JSonBuilder(writer).json {
            keywords {
                for (k in keyword_list.result_list) {
                    keyword(value: k.value)
                }
            }
        }
        return writer.toString()
    }
}
