package org.jbei.wors.lib.part.sequence.formatters;

import org.jbei.wors.lib.dto.FeaturedDNASequence;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Interface for Formatters.
 *
 * @author Zinovii Dmytriv
 */
public interface IFormatter {
    /**
     * Interface method to take a {@link FeaturedDNASequence} object and output the formatted file to the
     * {@link OutputStream}.
     *
     * @param sequence
     * @param outputStream
     * @throws FormatterException
     * @throws IOException
     */
    void format(FeaturedDNASequence sequence, OutputStream outputStream) throws FormatterException, IOException;
}
