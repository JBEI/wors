package org.jbei.wors.services.rest;

import com.google.common.io.ByteStreams;
import org.jbei.wors.lib.part.sequence.ByteArrayWrapper;
import org.jbei.wors.lib.part.sequence.DynamicSequence;
import org.jbei.wors.lib.part.sequence.SequenceFormat;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.ByteArrayInputStream;

/**
 * Rest resource for handling file downloads
 *
 * @author Hector Plahar
 */
@Path("/file")
public class FileResource extends RestResource {
    @GET
    @Path("{recordId}/sequence/{type}")
    public Response downloadSequence(
            @PathParam("recordId") final String recordId,
            @PathParam("type") final String downloadType) {
        DynamicSequence sequence = new DynamicSequence(recordId);

        final ByteArrayWrapper wrapper = sequence.convert(SequenceFormat.fromString(downloadType));
        if (wrapper == null)
            return super.respond(null);

        StreamingOutput stream = output -> {
            final ByteArrayInputStream input = new ByteArrayInputStream(wrapper.getBytes());
            ByteStreams.copy(input, output);
        };

        return addHeaders(Response.ok(stream), wrapper.getName());
    }
}
