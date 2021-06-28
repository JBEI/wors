package org.jbei.wors.lib.dto.entry;

import org.jbei.wors.lib.dto.FeaturedDNASequence;
import org.jbei.wors.lib.dto.IDataTransferModel;
import org.jbei.wors.lib.part.PartSource;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Hector Plahar
 * <p>
 * Wrapper (POJO) for part data and sequence
 */
public class PartSequence implements IDataTransferModel {

    private PartData part;
    private FeaturedDNASequence sequence;
    private List<PartSequence> children;
    private PartSource partSource;

    public PartSequence() {
        this.children = new ArrayList<>();
    }

    public PartSequence(PartData partData, FeaturedDNASequence sequence) {
        this.part = partData;
        this.sequence = sequence;
        this.part.setHasSequence(this.sequence != null);
        this.children = new ArrayList<>();
    }

    public void setPart(PartData part) {
        this.part = part;
    }

    public void setSequence(FeaturedDNASequence sequence) {
        this.sequence = sequence;
    }

    public PartData getPart() {
        return this.part;
    }

    public FeaturedDNASequence getSequence() {
        return this.sequence;
    }

    public List<PartSequence> getChildren() {
        return this.children;
    }

    public PartSource getPartSource() {
        return partSource;
    }

    public void setPartSource(PartSource partSource) {
        this.partSource = partSource;
    }
}
