package org.jbei.wors.lib.part.sequence.formatters;

import org.jbei.wors.lib.dto.FeaturedDNASequence;
import org.jbei.wors.lib.dto.entry.PartSequence;
import org.sbolstandard.core2.SBOLConversionException;
import org.sbolstandard.core2.SBOLDocument;
import org.sbolstandard.core2.SBOLValidationException;

import java.io.OutputStream;
import java.net.URISyntaxException;

public class SBOL2Formatter extends AbstractFormatter {

    private final PartSequence partSequence;

    public SBOL2Formatter(PartSequence sequence) {
        this.partSequence = sequence;
    }

    @Override
    public void format(FeaturedDNASequence sequence, OutputStream outputStream) throws FormatterException {

        SBOLDocument doc = new SBOLDocument();

        try {
            SBOL2Visitor visitor = new SBOL2Visitor(doc);
            visitor.visit(this.partSequence);
            doc.write(outputStream);
        } catch (SBOLValidationException | SBOLConversionException | URISyntaxException e) {
            throw new FormatterException(e);
        }
    }
}