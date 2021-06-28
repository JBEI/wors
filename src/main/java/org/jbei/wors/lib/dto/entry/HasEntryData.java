package org.jbei.wors.lib.dto.entry;

import org.jbei.wors.lib.dto.IDataTransferModel;

public abstract class HasEntryData implements IDataTransferModel {

    private PartData entryInfo;

    public HasEntryData() {
    }

    public void setEntryInfo(PartData view) {
        this.entryInfo = view;
    }

    public PartData getEntryInfo() {
        return this.entryInfo;
    }
}
