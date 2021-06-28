package org.jbei.wors.lib.part;

import org.jbei.wors.lib.common.logging.Logger;
import org.jbei.wors.lib.dto.FeaturedDNASequence;
import org.jbei.wors.lib.dto.entry.PartData;
import org.jbei.wors.lib.dto.entry.PartSequence;
import org.jbei.wors.lib.dto.search.SearchResult;
import org.jbei.wors.lib.index.RemoteGenBankPart;
import org.jbei.wors.lib.index.SearchIndex;
import org.jbei.wors.lib.scrape.AddGeneParts;
import org.jbei.wors.lib.scrape.IgemParts;
import org.jbei.wors.lib.search.blast.Constants;
import org.jbei.wors.services.rest.RestClient;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Parts implements Iterator<PartSequence> {

    private final List<PartsProducer> sources = new ArrayList<>();
    private PartsProducer currentSource;
    private boolean hasNextCalled = false;

    // register a source of partSequences
    public void registerSource(PartsProducer source) {
        sources.add(source);
    }

    public PartSequence get(String recordId) {
        try {
            SearchIndex index = new SearchIndex();
            SearchResult searchResult = index.getByRecordId(recordId);
            if (searchResult == null)
                return null;

            String partnerUrl = searchResult.getPartner().getUrl();

            if (partnerUrl.startsWith(Constants.IGEM_PART_URL_PREFIX)) {
                return IgemParts.parseIgemPart(Constants.IGEM_XML_PART_URL_PREFIX + searchResult.getEntryInfo().getName());
            }

            if (partnerUrl.startsWith(Constants.NCBI_SEARCH_URL)) {
                RemoteGenBankPart remoteGenBankPart = new RemoteGenBankPart();
                FeaturedDNASequence sequence = remoteGenBankPart.getSequence(searchResult.getEntryInfo().getPartId());
                return new PartSequence(searchResult.getEntryInfo(), sequence);
            }

            if (partnerUrl.startsWith(Constants.ADDGENE_URL_PREFIX)) {
                PartSequence sequence = new AddGeneParts().retrievePlasmid(searchResult.getEntryInfo().getPartId());
                sequence.getPart().setRecordId(searchResult.getEntryInfo().getRecordId());
                return sequence;
            }

            if (partnerUrl.equalsIgnoreCase(Constants.MASTER_REGISTRY_URL)) {
                PartData partData = RestClient.getInstance().get(Constants.MASTER_REGISTRY_URL, "/rest/parts/" + recordId, PartData.class, null);
                FeaturedDNASequence sequence = null;
                if (partData.isHasSequence()) {
                    sequence = RestClient.getInstance().get(partnerUrl, "/rest/parts/" + recordId + "/sequence", FeaturedDNASequence.class, null);
                }
                return new PartSequence(partData, sequence);
            }

            PartData data = RestClient.getInstance().get(Constants.MASTER_REGISTRY_URL, "/rest/web/"
                    + searchResult.getPartner().getId()
                    + "/entries/" + searchResult.getEntryInfo().getId(), PartData.class, null);
            FeaturedDNASequence sequence = RestClient.getInstance().get(Constants.MASTER_REGISTRY_URL,
                    "/rest/partners/" + searchResult.getPartner().getId() + "/" + searchResult.getEntryInfo().getId()
                            + "/sequence", FeaturedDNASequence.class, null);
            return new PartSequence(data, sequence);
        } catch (Exception e) {
            Logger.error(e);
            return null;
        }
    }

    @Override
    public boolean hasNext() {
        hasNextCalled = true;

        if (currentSource == null) {
            if (sources.isEmpty())
                return false;

            currentSource = sources.remove(0);
        }

        boolean currentHasNext = currentSource.hasNext();

        // iterate until we find next available data from the sources or run out of sources
        while (!currentHasNext) {
            if (sources.isEmpty())
                return false;

            currentSource = sources.remove(0);
            currentHasNext = currentSource.hasNext();
        }

        return true;
    }

    public void skipCurrentPartSource() {
        hasNextCalled = false;
        this.currentSource = null;
    }

    @Override
    public PartSequence next() {
        if (!hasNextCalled)
            throw new IllegalStateException("Cannot get next() without call to hasNext()");

        PartSequence next = currentSource.next();
        hasNextCalled = false;
        return next;
    }
}
