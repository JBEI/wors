package org.jbei.wors.services.rest;

import org.jbei.wors.lib.common.logging.Logger;
import org.jbei.wors.lib.dto.FeaturedDNASequence;
import org.jbei.wors.lib.dto.Registry;
import org.jbei.wors.lib.dto.entry.PartSequence;
import org.jbei.wors.lib.dto.search.SearchQuery;
import org.jbei.wors.lib.dto.search.SearchResult;
import org.jbei.wors.lib.index.RemoteGenBankPart;
import org.jbei.wors.lib.index.SearchIndex;
import org.jbei.wors.lib.part.Parts;
import org.jbei.wors.lib.scrape.AddGeneParts;
import org.jbei.wors.lib.scrape.IgemParts;
import org.jbei.wors.lib.search.blast.Constants;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * REST resource for searching. Supports keyword search with query params for filtering and advanced search
 *
 * @author Hector Plahar
 */
@Path("/search")
public class SearchResource extends RestResource {

    @Path("/{partId}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response findPart(@PathParam("partId") String partId) {
        Parts parts = new Parts();
        return super.respond(parts.get(partId));
    }

    @Path("/{partId}/sequence")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response findPartSequence(@PathParam("partId") String partId) {
        try {
            SearchIndex index = new SearchIndex();
            SearchResult searchResult = index.getByRecordId(partId);
            if (searchResult == null)
                return super.respond(null);

            final String url = searchResult.getPartner().getUrl();

            if (url.startsWith(Constants.IGEM_PART_URL_PREFIX)) {
                PartSequence sequence = IgemParts.parseIgemPart(Constants.IGEM_XML_PART_URL_PREFIX + searchResult.getEntryInfo().getName());
                if (sequence == null)
                    return super.respond(null);

                return super.respond(sequence.getSequence());
            }

            if (url.startsWith(Constants.ADDGENE_URL_PREFIX)) {
                PartSequence sequence = new AddGeneParts().retrievePlasmid(searchResult.getEntryInfo().getPartId());
                return super.respond(sequence.getSequence());
            }

            if (url.startsWith(Constants.NCBI_SEARCH_URL)) {
                RemoteGenBankPart remoteGenBankPart = new RemoteGenBankPart();
                return super.respond(remoteGenBankPart.getSequence(searchResult.getEntryInfo().getPartId()));
            }

            if (url.equalsIgnoreCase(Constants.MASTER_REGISTRY_URL)) {
                FeaturedDNASequence sequence = RestClient.getInstance().get(url, "/rest/parts/" + partId + "/sequence", FeaturedDNASequence.class, null);
                return super.respond(sequence);
            }

            // todo : this endpoint will most likely be updated
            FeaturedDNASequence sequence = RestClient.getInstance().get(Constants.MASTER_REGISTRY_URL,
                    "/rest/partners/" + searchResult.getPartner().getId() + "/" + searchResult.getEntryInfo().getId()
                            + "/sequence", FeaturedDNASequence.class, null);
            return super.respond(sequence);
        } catch (Exception e) {
            Logger.error(e);
            return super.respond(null);
        }
    }

    /**
     * Advanced Search. The use of post is mostly for the sequence string for blast which can get
     * very long and results in a 413 status code if sent via GET
     *
     * @param query parameters to the search
     * @return results of the search
     */
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response search(SearchQuery query) {
        SearchIndex index = new SearchIndex();
        return super.respond(index.find(query));
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/registries")
    public Response addRegistry(Registry registry) {
        // write registry data to file
        Logger.info("New registry add request");
        Logger.info(registry.getUserName() + "<" + registry.getUserEmail() + ">");
        Logger.info(registry.getName());
        Logger.info(registry.getUrl());
        Logger.info(registry.getDetails());
        return Response.status(Response.Status.OK).build();
    }
}
