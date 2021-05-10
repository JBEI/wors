package org.jbei.wors.lib.part.sequence;

/**
 * Different sequence formats supported by ICE
 *
 * @author Hector Plahar
 */
public enum SequenceFormat {
    ORIGINAL,
    FASTA,
    SBOL,
    PLAIN,
    GFF3,
    GENBANK;

    public static SequenceFormat fromString(String type) {
        return SequenceFormat.valueOf(type.toUpperCase());
    }
}