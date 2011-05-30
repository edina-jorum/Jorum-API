/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 *
 *
 *  University Of Edinburgh (EDINA) 
 *  Scotland
 *
 *
 *  File Name           : CollectionController.groovy
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

class CollectionController {
    def repositoryService
    private def appName     = grailsApplication.config.grails.app.name
    private def appVersion  = grailsApplication.config.grails.app.context
    private def analyticsId = grailsApplication.config.google.analytics.webPropertyID
    
    private JGoogleAnalyticsTracker tracker = new JGoogleAnalyticsTracker(appName,appVersion,analyticsId)
    
    def index = {
        redirect(action: "list")
    }
    
    def list = {
        try {
            def collection_list = repositoryService.listCollections()
            FocusPoint focusPoint
            withFormat {
                xml{ 
                    focusPoint = new FocusPoint("${controllerName}/${actionName}/" + params.id + ".xml");
                    tracker.trackAsynchronously(focusPoint);
                    def xml = toXML(collection_list)
                    render(text:xml, contentType:"application/xml", encoding:"UTF-8")
                }
                json{
                    focusPoint = new FocusPoint("${controllerName}/${actionName}/" + params.id + ".json");
                    tracker.trackAsynchronously(focusPoint);
                    def json = toJSON(collection_list)
                    render(text:json, contentType:"application/json", encoding:"UTF-8")
                }
                jsonp{
                    if (params.callback != null){
                        focusPoint = new FocusPoint("${controllerName}/${actionName}/" + params.id + ".jsonp");
                        tracker.trackAsynchronously(focusPoint);
                        def json = toJSON(collection_list)
                        render(text:"${params.callback}(${json})", contentType:"application/javascript", encoding:"UTF-8")
                    } else {
                        render "Please supply a callback parameter (e.g. callback=myFunc) in your request to use the JSONP service."
                    }
                }
                rss{
                    render "The collection list cannot be rendered as an RSS feed."
                }
            }
        } catch (NullPointerException e){
            render "Sorry, this service is currently unavailable."
        }
    }
    
    private def toXML = { collection_list ->
        def writer = new StringWriter()
        def xml    = new MarkupBuilder(writer)
        xml.collections{
            for (c in collection_list) {
                collection {
                    id(c.id) 
                    name(c.name)
                }
            }        
        }
        return writer.toString()
    }
    
    private def toJSON = { collection_list ->
        def writer = new StringWriter();
        new JSonBuilder(writer).json {
            collections {
                for (c in collection_list) {
                    collection (id: c.id, name: c.name)
                }
            }
        }
        return writer.toString()
    }
}
