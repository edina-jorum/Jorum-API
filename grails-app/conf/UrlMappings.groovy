/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 *
 *
 *  University Of Edinburgh (EDINA) 
 *  Scotland
 *
 *
 *  File Name           : UrlMappings.groovy
 *  Author              : ianfieldhouse
 *  Approver            : Ian Fieldhouse 
 * 
 *  Notes               :
 *
 *
 *~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 *
 */

class UrlMappings {
    static mappings = {
      "/$controller/$action?/$id?"{
	      constraints {
			 // apply constraints here
		  }
	  }
      "/" {
			controller = "collection"
			action = "index"
		}
	  "500"(view:'/error')
	}
}
