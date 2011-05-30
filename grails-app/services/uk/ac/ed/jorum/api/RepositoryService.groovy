/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 *
 *
 *  University Of Edinburgh (EDINA) 
 *  Scotland
 *
 *
 *  File Name           : RepositoryService.groovy
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

import java.sql.SQLException
import org.dspace.browse.*
import org.dspace.content.Collection
import org.dspace.content.DCValue
import org.dspace.content.DSpaceObject
import org.dspace.content.Item
import org.dspace.content.ItemIterator
import org.dspace.core.Context
import org.dspace.eperson.EPerson
import org.dspace.handle.HandleManager
import org.dspace.search.DSQuery
import org.dspace.search.QueryArgs
import org.dspace.search.QueryResults
import org.dspace.sort.SortOption;
import org.dspace.sort.SortException;

class RepositoryService {

    static transactional = false
    static scope         = "request"
    
    def grailsApplication
    
    def listCollections() {
        
        Context context
        List<RepositoryCollection> collections = new ArrayList<RepositoryCollection>()
        
        try {
            context = new Context()
            Collection[] dspace_collections = Collection.findAll(context)
            for (Collection dspace_collection : dspace_collections)
            {
                String id       = dspace_collection.getID();
                String name     = dspace_collection.getName();

                def collection  = new RepositoryCollection()
                collection.id   = id
                collection.name = name

                collections.add(collection)
            }
        } catch (SQLException e){
            log.error ("Couldn't make connection to the datbase:\n", e)
        } finally {
            context.complete()
            return collections
        }
    }
    
    def listAuthors(int limit, String order, String start_letter, int offset) {
        List<RepositoryAuthor> authors = new ArrayList<RepositoryAuthor>()
        BrowseInfo browseInfo = configurableBrowse("author", limit, order, start_letter, offset)
        RepositorySearchResults browse_results = new RepositorySearchResults()
        browse_results.total          = browseInfo.getTotal()
        browse_results.limit          = browseInfo.getResultsPerPage()
        browse_results.offset         = browseInfo.getOffset()
        browse_results.nextOffset     = browseInfo.getNextOffset()
        browse_results.previousOffset = browseInfo.getPrevOffset()
        (browseInfo.isAscending()) ? (browse_results.order = "asc") : (browse_results.order = "desc")
        def author_list = browseInfo.getResults()
        author_list.each { author_record ->
            def author  = new RepositoryAuthor()
            author.name = author_record
            authors.add(author)
        }
        browse_results.result_list  = authors
        return browse_results
    }
    
   def listKeywords(int limit, String order, String start_letter, int offset) {
        List<RepositoryKeyword> keywords = new ArrayList<RepositoryKeyword>()
        BrowseInfo browseInfo = configurableBrowse("subject", limit, order, start_letter, offset)
        RepositorySearchResults browse_results = new RepositorySearchResults()
        browse_results.total          = browseInfo.getTotal()
        browse_results.limit          = browseInfo.getResultsPerPage()
        browse_results.offset         = browseInfo.getOffset()
        browse_results.nextOffset     = browseInfo.getNextOffset()
        browse_results.previousOffset = browseInfo.getPrevOffset()
        (browseInfo.isAscending()) ? (browse_results.order = "asc") : (browse_results.order = "desc")
        def keyword_list = browseInfo.getResults()
        keyword_list.each { keyword_record ->
            def keyword  = new RepositoryKeyword()
            keyword.value = keyword_record
            keywords.add(keyword)
        }
        browse_results.result_list  = keywords
        return browse_results
    }
    
    def browseResources(String type, String search_term, int limit, String order, String start_letter, int offset) {
        Context context = new Context()
        
        Collection collection = null
        if (type.equals("collection")){
            type="title";
            if (!search_term.equals("")){
                collection = Collection.find(context, Integer.parseInt(search_term))
            }
        } 
        if (type.equals("keyword")){type = "subject"}
        if (type.equals("index")){type = "dateissued"}
        
        List<RepositoryResource> resource_list = new ArrayList<RepositoryResource>()
        BrowseInfo browseInfo = configurableBrowse(type, limit, order, start_letter, search_term, collection, offset)
        
        RepositorySearchResults browse_results = new RepositorySearchResults()
        browse_results.total          = browseInfo.getTotal()
        browse_results.limit          = browseInfo.getResultsPerPage()
        browse_results.offset         = browseInfo.getOffset()
        browse_results.nextOffset     = browseInfo.getNextOffset()
        browse_results.previousOffset = browseInfo.getPrevOffset()
        (browseInfo.isAscending()) ? (browse_results.order = "asc") : (browse_results.order = "desc")
        
        def results_list = browseInfo.getResults()
        
        for (BrowseItem resource_record : results_list ) {
            int id               = resource_record.getID()
            DSpaceObject item    = (DSpaceObject)Item.find(context, id)

            def resource         = new RepositoryResource()
            resource.id          = id
            resource.title       = item.getName()
            resource.url         = item.getHandle()
            resource.description = getItemMetadata(item, "description", null)
            resource.authors     = getItemMetadata(item, "contributor", "author")
            resource.keywords    = getItemMetadata(item, "subject", null)
            resource.rights_desc = getItemMetadata(item, "rights", null)
            resource.rights_url  = getItemMetadata(item, "rights", "uri")

            resource_list.add(resource)
        }
        context.complete()
        browse_results.result_list  = resource_list
        return browse_results
    }
	
	def findResourcesBySubmitter(String submitter_email) {
        RepositorySearchResults browse_results = new RepositorySearchResults()
        
        List<RepositoryResource> resource_list = new ArrayList<RepositoryResource>()
        Context context = new Context()
        String email    = submitter_email
        EPerson eperson = EPerson.findByEmail(context, email)
        ItemIterator submitterItems = Item.findBySubmitter(context, eperson)
        
        List<DSpaceObject> itemList = new LinkedList<DSpaceObject>();
        try {
            while (submitterItems.hasNext()) {
                itemList.add(submitterItems.next());
            }
        }
        finally {
            if (submitterItems != null) {
                submitterItems.close();
            }
        }
        
        for (DSpaceObject resource_record : itemList){
            int id               = resource_record.getID()
            String title         = resource_record.getName()
            String url           = resource_record.getHandle()
            
            def resource         = new RepositoryResource()
            resource.id          = id
            resource.title       = title
            resource.url         = url
            resource.description = getItemMetadata(resource_record, "description", null)
            resource.authors     = getItemMetadata(resource_record, "contributor", "author")
            resource.keywords    = getItemMetadata(resource_record, "subject", null)
            resource.rights_desc = getItemMetadata(resource_record, "rights", null)
            resource.rights_url  = getItemMetadata(resource_record, "rights", "uri")

            resource_list.add(resource)
        }
        browse_results.result_list  = resource_list
        return browse_results
    }

    def findResourcesBySearchTerm(String search_term, int limit) {
        RepositorySearchResults browse_results = new RepositorySearchResults()
        
        def max_limit     = grailsApplication.config.results.limit
        def default_limit = grailsApplication.config.results.default
        
        List<RepositoryResource> resource_list = new ArrayList<RepositoryResource>()
        
        Context context = new Context()
        String query = search_term
        
        QueryArgs queryArgs = new QueryArgs()
        
        if (limit < max_limit) {
            queryArgs.setPageSize(limit);
        } else {
            queryArgs.setPageSize(default_limit);
        }
        
        queryArgs.setQuery(query)
        
        QueryResults queryResults = null;
        queryResults = DSQuery.doQuery(context, queryArgs);
        
        List<String> handles = queryResults.getHitHandles()
        for (String handle : handles) {
            DSpaceObject resource_record = HandleManager.resolveToObject(context, handle)
            
            int id               = resource_record.getID()
            String title         = resource_record.getName()
            
            def resource         = new RepositoryResource()
            resource.id          = id
            resource.title       = title
            resource.url         = handle
            resource.description = getItemMetadata(resource_record, "description", null)
            resource.authors     = getItemMetadata(resource_record, "contributor", "author")
            resource.keywords    = getItemMetadata(resource_record, "subject", null)
            resource.rights_desc = getItemMetadata(resource_record, "rights", null)
            resource.rights_url  = getItemMetadata(resource_record, "rights", "uri")

            resource_list.add(resource)
        }
        
        browse_results.result_list  = resource_list
        return browse_results
    }

    def configurableBrowse(String type, int limit, String order=null, String start_letter=null, String filter=null, Collection collection=null, int offset) {
        Context context = new Context()
        BrowserScope scope = new BrowserScope(context)
        BrowseIndex bi = BrowseIndex.getBrowseIndex(type)
        int sortBy = -1;
        // If we don't have a sort column
        if (sortBy == -1)
        {
            // Get the default one
            SortOption so = bi.getSortOption();
            
            if (so != null)
            {
                sortBy = so.getNumber();
            }
        }
        else if (bi.isItemIndex() && !bi.isInternalIndex())
        {
            try
            {
                // If a default sort option is specified by the index, but it isn't
                // the same as sort option requested, attempt to find an index that
                // is configured to use that sort by default
                // This is so that we can then highlight the correct option in the navigation
                SortOption bso = bi.getSortOption();
                SortOption so = SortOption.getSortOption(sortBy);
                if ( bso != null && bso != so)
                {
                    BrowseIndex newBi = BrowseIndex.getBrowseIndex(so);
                    if (newBi != null)
                    {
                        bi   = newBi;
                        type = bi.getName();
                    }
                }
            }
            catch (SortException se)
            {
                log.error ("Unable to get sort options:\n", se)
            }
        }
        scope.setBrowseIndex(bi)
        scope.setCollection(collection)
        scope.setCommunity(null)
        scope.setEtAl(0)
        if (collection)
			scope.setFilterValue(null)
		else
			scope.setFilterValue(filter)
        scope.setFilterValueLang(null)
        scope.setFilterValuePartial(false)
        scope.setJumpToItem(-1)
        scope.setJumpToValue(null)
        scope.setJumpToValueLang(null)
        if (filter != null)
            scope.setBrowseLevel(1)
        else
            scope.setBrowseLevel(0)
        scope.setOffset(offset)
        scope.setOrder(order)
        scope.setResultsPerPage(limit)
        scope.setSortBy(sortBy)
        scope.setStartsWith(start_letter)

        BrowseInfo browseInfo
        try
        {
            // Create a new browse engine, and perform the browse
            BrowseEngine be = new BrowseEngine(context);
            browseInfo = be.browse(scope);
            
            // there is no limit, or the UI says to use the default
            browseInfo.setEtAl(-1);

        }
        catch (BrowseException bex)
        {
            log.error ("Error whilst creating Browse:\n", bex)
        }
        finally
        {
            context.complete()
            return browseInfo
        }
    }
    
    private def getItemMetadata(DSpaceObject resource_record, String metadata_element, String qualifier) {
        try
        {
            DCValue[] metadata_item = resource_record.getMetadata("dc", metadata_element, qualifier, "*")
            def num_items = metadata_item.length
            if (num_items == 1)
            return metadata_item[0].value
            else {
                def items = []
                metadata_item.each(){ items.add(it.value) }
                return items
            }

        }
        catch (SQLException e)
        {
            log.error("caught exception: ", e)
            return null;
        }
    }
}
