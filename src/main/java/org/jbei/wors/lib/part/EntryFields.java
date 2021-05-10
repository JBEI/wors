package org.jbei.wors.lib.part;

import org.jbei.wors.lib.dto.entry.EntryField;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Hector Plahar
 */
public class EntryFields {

    public static List<EntryField> getCommonFields() {
        List<EntryField> list = new ArrayList<>();
        list.add(EntryField.PI);
        list.add(EntryField.PI_EMAIL);
        list.add(EntryField.FUNDING_SOURCE);
        list.add(EntryField.IP);
        list.add(EntryField.BIO_SAFETY_LEVEL);
        list.add(EntryField.NAME);
        list.add(EntryField.ALIAS);
        list.add(EntryField.KEYWORDS);
        list.add(EntryField.SUMMARY);
        list.add(EntryField.NOTES);
        list.add(EntryField.REFERENCES);
        list.add(EntryField.LINKS);
        list.add(EntryField.STATUS);
        list.add(EntryField.CREATOR);
        list.add(EntryField.CREATOR_EMAIL);
        return list;
    }

    public static void addStrainHeaders(List<EntryField> list) {
        list.add(EntryField.HOST);
        list.add(EntryField.GENOTYPE_OR_PHENOTYPE);
        list.add(EntryField.SELECTION_MARKERS);
    }

    public static void addPlasmidHeaders(List<EntryField> list) {
        list.add(EntryField.CIRCULAR);
        list.add(EntryField.BACKBONE);
        list.add(EntryField.PROMOTERS);
        list.add(EntryField.REPLICATES_IN);
        list.add(EntryField.ORIGIN_OF_REPLICATION);
        list.add(EntryField.SELECTION_MARKERS);
    }

    public static void addArabidopsisSeedHeaders(List<EntryField> list) {
        list.add(EntryField.HOMOZYGOSITY);
        list.add(EntryField.HARVEST_DATE);
        list.add(EntryField.ECOTYPE);
        list.add(EntryField.PARENTS);
        list.add(EntryField.GENERATION);
        list.add(EntryField.PLANT_TYPE);
        list.add(EntryField.SELECTION_MARKERS);
        list.add(EntryField.SENT_TO_ABRC);
    }

    public static void addProteinHeaders(List<EntryField> list) {
        list.add(EntryField.ORGANISM);
        list.add(EntryField.FULL_NAME);
        list.add(EntryField.GENE_NAME);
        list.add(EntryField.UPLOADED_FROM);
    }
}
