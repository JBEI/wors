package org.jbei.wors.lib.part.sequence.formatters;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;


/**
 * @author Hector Plahar
 */
public class IceSequenceOntology {

    private static Map<String, String> map = new HashMap<>();

    static {
        map.put("misc_feature", "SO:0000001");
        map.put("misc_structure", "SO:0000002");
        map.put("satellite", "SO:0000005");
        map.put("scRNA", "SO:0000013");
        map.put("stem_loop", "SO:0000019");
        map.put("operator", "SO:0000057");
        map.put("protein", "SO:0000104");
        map.put("primer", "SO:0000112");
        map.put("RBS", "SO:0000139");
        map.put("attenuator", "SO:0000140");
        map.put("terminator", "SO:0000141");
        map.put("exon", "SO:0000147");
        map.put("source", "SO:0000149");
        map.put("plasmid", "SO:0000155");
        map.put("enhancer", "SO:0000165");
        map.put("promoter", "SO:0000167");
        map.put("CAAT_signal", "SO:0000172");
        map.put("GC_signal", "SO:0000173");
        map.put("TATA_signal", "SO:0000174");
        map.put("-10_signal", "SO:0000175");
        map.put("-35_signal", "SO:0000176");
        map.put("precursor_RNA", "SO:0000185");
        map.put("prim_transcript", "SO:0000185");
        map.put("intron", "SO:0000188");
        map.put("5'UTR", "SO:0000204");
        map.put("3'UTR", "SO:0000205");
        map.put("misc_RNA", "SO:0000233");
        map.put("mRNA", "SO:0000234");
        map.put("rRNA", "SO:0000252");
        map.put("tRNA", "SO:0000253");
        map.put("snRNA", "SO:0000274");
        map.put("LTR", "SO:0000286");
        map.put("rep_origin", "SO:0000296");
        map.put("D-loop", "SO:0000297");
        map.put("misc_recomb", "SO:0000298");
        map.put("modified_base", "SO:0000305");
        map.put("CDS", "SO:0000316");
        map.put("start", "SO:0000323");
        map.put("tag", "SO:0000324");
        map.put("stop", "SO:0000327");
        map.put("STS", "SO:0000331");
        map.put("misc_binding", "SO:0000409");
        map.put("protein_bind", "SO:0000410");
        map.put("misc_difference", "SO:0000413");
        map.put("protein_domain", "SO:0000417");
        map.put("sig_peptide", "SO:0000418");
        map.put("mat_peptide", "SO:0000419");
        map.put("D_segment", "SO:0000458");
        map.put("J_region", "SO:0000470");
        map.put("polyA_signal", "SO:0000551");
//        map.put("RBS", "SO:0000552"); // TODO : need to add for RBS and use 0000139 as default
        map.put("polyA_site", "SO:0000553");
        map.put("5'clip", "SO:0000555");
        map.put("3'clip", "SO:0000557");
        map.put("repeat_region", "SO:0000657");
        map.put("gene", "SO:0000704");
        map.put("iDNA", "SO:0000723");
        map.put("transit_peptide", "SO:0000725");
        map.put("repeat_unit", "SO:0000726");
        map.put("conserved", "SO:0000856");
        map.put("s_mutation", "SO:0001017");
        map.put("allele", "SO:0001023");
        map.put("transposon", "SO:0001054");
        map.put("variation", "SO:0001060");
        map.put("misc_marker", "SO:0001645");
        map.put("V_region", "SO:0001833");
        map.put("C_region", "SO:0001834");
        map.put("N_region", "SO:0001835");
        map.put("S_region", "SO:0001836");
        map.put("misc_signal", "SO:0005836");
        map.put("primer_bind", "SO:0005850");
    }

    static URI getURI(String type) {
        String soNum;
        if ("RBS".equalsIgnoreCase(type.trim()))
            soNum = "SO:0000552";
        else
            soNum = map.get(type);
        if (soNum == null)
            soNum = "SO:0000001";

        return URI.create("http://identifiers.org/so/" + soNum);
    }
}
