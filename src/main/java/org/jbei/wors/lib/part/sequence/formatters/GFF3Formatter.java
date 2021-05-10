package org.jbei.wors.lib.part.sequence.formatters;

import org.apache.commons.lang3.StringUtils;
import org.jbei.wors.lib.dto.DNAFeature;
import org.jbei.wors.lib.dto.DNAFeatureLocation;
import org.jbei.wors.lib.dto.FeaturedDNASequence;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

/**
 * Formatter for GFF3 Format
 *
 * @author Hector Plahar
 */
public class GFF3Formatter extends AbstractFormatter {

    private final String[] HEADERS = new String[]{
            "seqid", "source", "type", "start", "end", "score", "strand", "phase", "attributes"
    };

    @Override
    public void format(FeaturedDNASequence sequence, OutputStream outputStream) throws FormatterException, IOException {
        if (sequence == null)
            throw new IllegalArgumentException("Cannot write null sequence");

        StringBuilder builder = new StringBuilder();

        builder.append("##gff-version 3.2.1").append(System.lineSeparator());

        // add headers
        builder.append("##");
        for (String header : HEADERS) {
            builder.append(header).append("\t");
        }
        builder.append(System.lineSeparator());

        String sequenceId = sequence.getIdentifier();
        sequenceId = sequenceId.replaceAll("[^a-zA-Z0-9.:^*$@!+_?-|]", "_");
        List<DNAFeature> featureSet = sequence.getFeatures();
        if (featureSet != null) {
            for (DNAFeature sequenceFeature : featureSet) {
                String featureLine = sequenceFeature.getName() + " ICE " + sequenceFeature.getType();

                // location
                for (DNAFeatureLocation location : sequenceFeature.getLocations()) {
                    builder.append(sequenceId).append("\t")
                            .append(".").append("\t")
                            .append(getColumn3(sequenceFeature.getType())).append("\t")
                            .append(location.getGenbankStart()).append("\t")
                            .append(location.getEnd()).append("\t")
                            .append(".").append("\t")
                            .append(sequenceFeature.getStrand() == 1 ? "+" : "-").append("\t")
                            .append(".").append("\t")
                            .append("ID=").append(featureLine);
                    builder.append(System.lineSeparator());
                }
            }
        }

        outputStream.write(builder.toString().getBytes());
    }

    protected String getColumn3(String genbankType) {
        if (StringUtils.isEmpty(genbankType))
            return "region";

        switch (genbankType.toLowerCase()) {
            case "mutation":
                return "sequence variant obs";
            case "gene":
                return "gene";

            default:
                return "region";
        }
    }
}
