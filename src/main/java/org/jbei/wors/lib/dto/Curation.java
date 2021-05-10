package org.jbei.wors.lib.dto;

/**
 * @author Hector Plahar
 */
public class Curation implements IDataTransferModel {

    private boolean exclude;

    public boolean isExclude() {
        return exclude;
    }

    public void setExclude(boolean exclude) {
        this.exclude = exclude;
    }
}
