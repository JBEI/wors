package org.jbei.wors.lib.search;

import org.jbei.wors.lib.dto.IDataTransferModel;
import org.jbei.wors.lib.dto.search.SearchQuery;

import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper around search results received from a remote ICE instance
 *
 * @author Hector Plahar
 */
public class WebSearchResults implements IDataTransferModel {

    private long totalCount;
    private List<WebResult> results;
    private SearchQuery query;

    public WebSearchResults(int numberOfPartners) {
        this.results = new ArrayList<>(numberOfPartners);
    }

    public long getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(long totalCount) {
        this.totalCount = totalCount;
    }

    public List<WebResult> getResults() {
        return results;
    }

    public SearchQuery getQuery() {
        return query;
    }

    public void setQuery(SearchQuery query) {
        this.query = query;
    }
}
