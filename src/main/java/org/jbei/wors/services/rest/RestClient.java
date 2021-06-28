package org.jbei.wors.services.rest;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.jbei.wors.lib.search.blast.Constants;

import javax.ws.rs.client.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Map;

/**
 * ICE REST client
 *
 * @author Hector Plahar
 */
public class RestClient {

    private static RestClient INSTANCE = new RestClient();
    private Client client;

    public static RestClient getInstance() {
        return INSTANCE;
    }

    protected RestClient() {
        ClientConfig clientConfig = new ClientConfig();
        clientConfig.register(PartDataJSONHandler.class);
        clientConfig.register(ArrayDataJSONHandler.class);
        clientConfig.register(MultiPartFeature.class);
        client = ClientBuilder.newClient(clientConfig);
    }

    public <T> T get(String url, String path, Class<T> clazz, Map<String, Object> queryParams) {
        WebTarget target = client.target("https://" + url).path(path);
        if (queryParams != null) {
            for (Map.Entry<String, Object> entry : queryParams.entrySet()) {
                target = target.queryParam(entry.getKey(), entry.getValue());
            }
        }
        Invocation.Builder invocationBuilder = target.request(MediaType.APPLICATION_JSON_TYPE);
        setHeaders(invocationBuilder);
        return invocationBuilder.buildGet().invoke(clazz);
    }

    // post to Wor
    public <T> T post(String url, String resourcePath, Object object, Class<T> responseClass,
                         Map<String, Object> queryParams) {
        WebTarget target = client.target("https://" + url).path(resourcePath);
        if (queryParams != null) {
            for (Map.Entry<String, Object> entry : queryParams.entrySet()) {
                target = target.queryParam(entry.getKey(), entry.getValue());
            }
        }

        Invocation.Builder invocationBuilder = target.request(MediaType.APPLICATION_JSON_TYPE);
        setHeaders(invocationBuilder);
        Response postResponse = invocationBuilder.post(Entity.entity(object, MediaType.APPLICATION_JSON_TYPE));
        if (postResponse.hasEntity() && postResponse.getStatus() == Response.Status.OK.getStatusCode())
            return postResponse.readEntity(responseClass);
        return null;
    }

    protected void setHeaders(Invocation.Builder invocationBuilder) {
        invocationBuilder.header("X-ICE-API-Token", Constants.PUBLIC_REGISTRY_TOKEN);
        invocationBuilder.header("X-ICE-API-Token-Client", Constants.PUBLIC_REGISTRY_TOKEN_CLIENT);
        invocationBuilder.header("X-ICE-API-Token-Owner", Constants.PUBLIC_REGISTRY_TOKEN_OWNER);
    }
}
