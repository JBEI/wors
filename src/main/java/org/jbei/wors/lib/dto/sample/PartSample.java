package org.jbei.wors.lib.dto.sample;

import org.jbei.wors.lib.account.AccountTransfer;
import org.jbei.wors.lib.dto.IDataTransferModel;
import org.jbei.wors.lib.dto.StorageLocation;
import org.jbei.wors.lib.dto.comment.UserComment;

import java.util.ArrayList;

/**
 * Parent class for the different types of samples
 *
 * @author Hector Plahar
 */
public class PartSample implements IDataTransferModel {

    private long id;
    private AccountTransfer depositor;
    private String label;
    private long creationTime;
    private boolean inCart;
    private StorageLocation location;
    private long partId;
    private String partName;
    private boolean canEdit;
    private ArrayList<UserComment> comments;

    public PartSample() {
        this.comments = new ArrayList<>();
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public AccountTransfer getDepositor() {
        return depositor;
    }

    public void setDepositor(AccountTransfer depositor) {
        this.depositor = depositor;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public long getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(long creationTime) {
        this.creationTime = creationTime;
    }

    public boolean isInCart() {
        return inCart;
    }

    public void setInCart(boolean inCart) {
        this.inCart = inCart;
    }

    public StorageLocation getLocation() {
        return location;
    }

    public void setLocation(StorageLocation location) {
        this.location = location;
    }

    public long getPartId() {
        return partId;
    }

    public void setPartId(long partId) {
        this.partId = partId;
    }

    public String getPartName() {
        return partName;
    }

    public void setPartName(String partName) {
        this.partName = partName;
    }

    public boolean isCanEdit() {
        return canEdit;
    }

    public void setCanEdit(boolean canEdit) {
        this.canEdit = canEdit;
    }

    public ArrayList<UserComment> getComments() {
        return comments;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        StorageLocation location = getLocation();
        while (location != null) {
            builder.append("[location: ").append(location.toString()).append("]");
            location = location.getChild();
        }
        return builder.toString();
    }
}
