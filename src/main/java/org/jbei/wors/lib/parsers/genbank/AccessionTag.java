package org.jbei.wors.lib.parsers.genbank;

/**
 * @author Hector Plahar
 */
public class AccessionTag extends Tag {

    public AccessionTag(String value) {
        super(Type.ACCESSION);
        this.setValue(value);
    }
}
