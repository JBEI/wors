package org.jbei.wors.services.rest.multipart;

import org.glassfish.jersey.media.multipart.MultiPartFeature;

import javax.ws.rs.core.Application;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Hector Plahar
 */
public class WorsApplication extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        final Set<Class<?>> classes = new HashSet<>();
        // register resources and features
        classes.add(MultiPartFeature.class);
        return classes;
    }
}
