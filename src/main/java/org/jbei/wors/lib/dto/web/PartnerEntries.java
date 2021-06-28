package org.jbei.wors.lib.dto.web;

import org.jbei.wors.lib.dto.IDataTransferModel;
import org.jbei.wors.lib.dto.common.Results;
import org.jbei.wors.lib.dto.entry.PartData;

/**
 * @author Hector Plahar
 */
public class PartnerEntries implements IDataTransferModel {

    private RegistryPartner partner;
    private Results<PartData> entries;

    public PartnerEntries(RegistryPartner partner, Results<PartData> dataResults) {
        this.partner = partner;
        this.entries = dataResults;
    }

    public RegistryPartner getPartner() {
        return partner;
    }

    public void setPartner(RegistryPartner partner) {
        this.partner = partner;
    }

    public Results<PartData> getEntries() {
        return entries;
    }

    public void setEntries(Results<PartData> entries) {
        this.entries = entries;
    }
}
