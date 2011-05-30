/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 *
 *
 *  University Of Edinburgh (EDINA) 
 *  Scotland
 *
 *
 *  File Name           : RepositorySearchResults.groovy
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

class RepositorySearchResults {
    int total
    int limit
    int offset
    int nextOffset
    int previousOffset
    String order
    def result_list
}