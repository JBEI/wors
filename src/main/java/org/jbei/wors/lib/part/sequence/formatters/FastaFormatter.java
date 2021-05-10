package org.jbei.wors.lib.part.sequence.formatters;

import org.jbei.wors.lib.dto.FeaturedDNASequence;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Formatter for creating a FASTA formatted output.
 * <p>
 *
 * @author Hector Plahar
 */
public class FastaFormatter extends AbstractFormatter {

    @Override
    public void format(FeaturedDNASequence sequence, OutputStream outputStream) throws FormatterException,
            IOException {
        if (sequence == null)
            throw new IllegalArgumentException("Cannot write null sequence");

        StringBuilder builder = new StringBuilder();
        builder.append(">")
                .append(sequence.getIdentifier())
                .append(System.lineSeparator());
        for (int i = 1; i <= sequence.getSequence().length(); i += 1) {
            builder.append(sequence.getSequence().charAt(i - 1));
            if (i % 80 == 0)
                builder.append(System.lineSeparator());
        }

        outputStream.write(builder.toString().getBytes());
    }
}
