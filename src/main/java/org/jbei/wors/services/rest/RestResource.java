package org.jbei.wors.services.rest;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

/**
 * Parent class for all rest resource objects.
 * Handles session validation as well as return responses based on specific values
 *
 * @author Hector Plahar
 */
public class RestResource {

    @Context
    protected HttpServletRequest request;

    /**
     * Create a {@link Response} object from an entity object.
     *
     * @param object entity in response
     * @return a 404 NOT FOUND if object is {@code null}, else a 200 OK response with the entity
     */
    protected Response respond(final Object object) {
        if (object == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        return Response.status(Response.Status.OK).entity(object).build();
    }

    protected Response addHeaders(Response.ResponseBuilder response, String fileName) {
        response.header("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
        int dotIndex = fileName.lastIndexOf('.') + 1;
        if (dotIndex == 0)
            return response.build();

        response.header("Content-Type", "application/octet-stream; name=\"" + fileName + "\"");
        return response.build();
    }
}
