package org.jbei.wors.lib.parsers;

import org.jbei.wors.lib.dto.FeaturedDNASequence;

import java.io.IOException;

public abstract class AbstractParser {

    protected final String userId;

    public AbstractParser() {
        this.userId = null;
    }

    public FeaturedDNASequence parse(String textSequence, String... entryType) throws IOException {
        throw new UnsupportedOperationException("Not implemented for this parser");
    }

    /**
     * Replace different line termination characters with the newline character (\n).
     *
     * @param sequence Text to clean.
     * @return String with only newline character (\n).
     */
    protected String cleanSequence(String sequence) {
        sequence = sequence.trim();
        sequence = sequence.replace("\n\n", "\n"); // *nix
        sequence = sequence.replace("\n\r\n\r", "\n"); // win
        sequence = sequence.replace("\r\r", "\n"); // mac
        sequence = sequence.replace("\n\r", "\n"); // *win
        return sequence;
    }
}
