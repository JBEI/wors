package org.jbei.wors.lib.dto.search;

import org.jbei.wors.lib.dto.IDataTransferModel;

/**
 * Represents the different kinds of indexes available
 *
 * @author Hector Plahar
 */
public enum IndexType implements IDataTransferModel {

    BLAST,
    LUCENE
}
