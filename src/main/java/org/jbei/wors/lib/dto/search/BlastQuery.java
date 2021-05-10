package org.jbei.wors.lib.dto.search;

import org.jbei.wors.lib.dto.IDataTransferModel;

/**
 * Blast query. Program type defaults to blastn
 *
 * @author Hector Plahar
 */
public class BlastQuery implements IDataTransferModel {

    public static final long serialVersionUID = 1L;

    private BlastProgram blastProgram;
    private String sequence;

    // required no arg constructor
    public BlastQuery() {
        blastProgram = BlastProgram.BLAST_N;
    }

    public BlastProgram getBlastProgram() {
        return blastProgram;
    }

    public void setBlastProgram(BlastProgram blastProgram) {
        this.blastProgram = blastProgram;
    }

    public String getSequence() {
        return sequence;
    }

    public void setSequence(String sequence) {
        this.sequence = sequence;
    }
}
