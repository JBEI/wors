package org.jbei.wors.lib.part;

import org.jbei.wors.lib.dto.entry.PartSequence;

/**
 * Produces
 */
public interface PartsProducer {

    boolean hasNext();

    PartSequence next();
}
