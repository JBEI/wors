package org.jbei.wors.lib.dto.entry;

import org.jbei.wors.lib.dto.IDataTransferModel;

/**
 * The results of an entry identifier that was parsed in order to retrieve it.
 * If the identifier is valid, then the PartData object is not null
 *
 * @author Hector Plahar
 */
public class ParsedEntryId implements IDataTransferModel {

    private final String identifier;
    private final PartData partData;

    public ParsedEntryId(String identifier, PartData partData) {
        this.identifier = identifier;
        this.partData = partData;
    }

    public String getIdentifier() {
        return identifier;
    }

    public PartData getPartData() {
        return partData;
    }
}
