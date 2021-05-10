package org.jbei.wors.lib.scrape;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.lang3.StringUtils;
import org.jbei.wors.lib.common.logging.Logger;
import org.jbei.wors.lib.dto.FeaturedDNASequence;
import org.jbei.wors.lib.dto.entry.PartData;
import org.jbei.wors.lib.dto.entry.PartSequence;
import org.jbei.wors.lib.dto.search.SearchQuery;
import org.jbei.wors.lib.dto.search.SearchResult;
import org.jbei.wors.lib.dto.search.SearchResults;
import org.jbei.wors.lib.dto.web.PartnerEntries;
import org.jbei.wors.lib.dto.web.RegistryPartner;
import org.jbei.wors.lib.part.PartSource;
import org.jbei.wors.lib.part.PartsProducer;
import org.jbei.wors.lib.search.blast.Constants;
import org.jbei.wors.services.rest.RestClient;

import java.lang.reflect.Type;
import java.util.*;

public class IceParts implements PartsProducer {

    private List<RegistryPartner> partners;
    private List<PartSequence> parts;
    private final PartSource source;
    private int start;
    private final int fetchCount = 30;
    private RegistryPartner nextPartner;

    public static void main(String[] args) {
        IceParts parts = new IceParts();
        int i = 0;
        while (parts.hasNext()) {
            PartSequence sequence = parts.next();
            System.out.println(sequence.getPart().getName());
            if (++i > 10)
                break;
        }
    }

    public IceParts() {

        RestClient client = RestClient.getInstance();

        // todo : ice should return an object
        List<RegistryPartner> partnersMap = client.get(Constants.MASTER_REGISTRY_URL, "/rest/partners", ArrayList.class, null);
        final Type type = new TypeToken<ArrayList<RegistryPartner>>() {
        }.getType();
        final Gson gson = new GsonBuilder().create();
        partners = gson.fromJson(gson.toJsonTree(partnersMap), type);
        if (partners == null) {
            partners = new ArrayList<>();
        }

        parts = new LinkedList<>();
        source = new PartSource(Constants.MASTER_REGISTRY_URL, Constants.MASTER_REGISTRY_NAME, null);
        nextPartner = new RegistryPartner();
        nextPartner.setUrl(Constants.MASTER_REGISTRY_URL);
        fetchMore();
    }

    public boolean hasNext() {
        return !parts.isEmpty();
    }

    public PartSequence next() {
        if (!hasNext())
            return null;

        PartSequence sequence = parts.remove(0);
        if (parts.isEmpty()) {
            fetchMore();
        }

        return sequence;
    }

    private void fetchMore() {
        if (nextPartner == null)
            return;

        int retrieved;
        if (nextPartner.getUrl().equalsIgnoreCase(Constants.MASTER_REGISTRY_URL)) {
            retrieved = fetchMasterEntries();
        } else {
            retrieved = fetchPartnerEntries(nextPartner);
        }

        if (retrieved == -1) {
            nextPartner = partners.isEmpty() ? null : partners.remove(0);
        }

        // set next start
        if (retrieved < fetchCount) {
            start = 0;
            nextPartner = partners.isEmpty() ? null : partners.remove(0);
        } else {
            start += retrieved;
        }
    }

    // returns number of fetched entries from master
    private int fetchMasterEntries() {
        SearchQuery query = new SearchQuery();
        query.getParameters().setStart(start);
        query.getParameters().setRetrieveCount(fetchCount);

        SearchResults results = RestClient.getInstance().post(Constants.MASTER_REGISTRY_URL, "/rest/search", query, SearchResults.class, null);
        if (results == null) {
            // todo
            Logger.error("No results from master: " + Constants.MASTER_REGISTRY_URL);
            return 0;
        }

        for (SearchResult result : results.getResults()) {
            PartSequence partSequence = new PartSequence();
            partSequence.setPartSource(source);
            partSequence.setPart(result.getEntryInfo());

            if (result.getEntryInfo().isHasSequence()) {
                // retrieve sequence
                FeaturedDNASequence sequence = RestClient.getInstance().get(Constants.MASTER_REGISTRY_URL, "/rest/parts/" + result.getEntryInfo().getId() + "/sequence", FeaturedDNASequence.class, null);
                if (sequence != null) {
                    partSequence.setSequence(sequence);
                }
            }

            parts.add(partSequence);
        }

        return results.getResults().size();
    }

    private int fetchPartnerEntries(RegistryPartner partner) {
        if (partner == null || StringUtils.isEmpty(partner.getUrl()))
            return -1;

        String url = partner.getUrl();
        String registryName = StringUtils.isEmpty(partner.getName()) ? url : partner.getName();

        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("offset", start);
        queryParams.put("limit", fetchCount);


        // fetch "limit" number of entries
        PartnerEntries results = RestClient.getInstance().get(Constants.MASTER_REGISTRY_URL, "/rest/partners/" + partner.getId() + "/entries", PartnerEntries.class, queryParams);
        if (results == null || results.getEntries().getResultCount() == 0) {
            Logger.error("No results from partner: " + partner.getUrl());
            return 0;
        }

        PartSource source = new PartSource(url, registryName, Long.toString(partner.getId()));

        // index each part and sequence (if available)
        for (PartData result : results.getEntries().getData()) {
            PartSequence partSequence = new PartSequence();
            partSequence.setPartSource(source);

            if (result.isHasSequence()) {
                // retrieve sequence
                FeaturedDNASequence sequence = RestClient.getInstance().get(Constants.MASTER_REGISTRY_URL, "/rest/web/" + partner.getId() + "/entries/" + result.getRecordId() + "/sequence", FeaturedDNASequence.class, null);
                if (sequence != null) {
                    partSequence.setSequence(sequence);
                }
            }

            parts.add(partSequence);
        }

        return results.getEntries().getData().size();
    }
}
