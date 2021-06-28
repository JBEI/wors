package org.jbei.wors.lib.part.sequence.formatters;

import org.biojava.bio.seq.DNATools;
import org.biojava.bio.seq.Feature;
import org.biojava.bio.symbol.Location;
import org.biojavax.RichAnnotation;
import org.biojavax.RichObjectFactory;
import org.biojavax.SimpleNote;
import org.biojavax.SimpleRichAnnotation;
import org.biojavax.bio.seq.*;
import org.biojavax.bio.seq.RichLocation.Strand;
import org.jbei.wors.lib.common.logging.Logger;
import org.jbei.wors.lib.dto.DNAFeature;
import org.jbei.wors.lib.dto.DNAFeatureLocation;
import org.jbei.wors.lib.dto.DNAFeatureNote;
import org.jbei.wors.lib.dto.FeaturedDNASequence;

import java.io.IOException;
import java.io.OutputStream;
import java.util.*;

/**
 * Formatter for the Genbank file format.
 *
 * @author Zinovii Dmytriv, Timothy Ham
 */
public class GenbankFormatter extends AbstractFormatter {
    private final String name;
    private final String accessionNumber;
    private int version = 1;
    private double seqVersion = 1.0;
    private boolean circular = false;
    private String description = "";
    private String division = "";
    private String identifier = "";

    /**
     * Constructor using only the name.
     *
     * @param name
     */
    public GenbankFormatter(String name) {
        this(name, name, 1, 1.0);
    }

    /**
     * Constructor using the complete header fields.
     *
     * @param name
     * @param accessionNumber
     * @param version
     * @param seqVersion
     */
    public GenbankFormatter(String name, String accessionNumber, int version, double seqVersion) {
        super();

        this.name = name;
        this.accessionNumber = accessionNumber;
        this.version = version;
        this.seqVersion = seqVersion;
    }

    public boolean getCircular() {
        return circular;
    }

    public void setCircular(boolean value) {
        circular = value;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDivision() {
        return division;
    }

    public void setDivision(String division) {
        this.division = division;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    @Override
    public void format(FeaturedDNASequence sequence, OutputStream outputStream) throws FormatterException, IOException {
        if (sequence == null || outputStream == null || sequence.getSequence().isEmpty()) {
            return;
        }

        SimpleRichSequence simpleRichSequence;
        try {
            simpleRichSequence = new SimpleRichSequence(
                    getNamespace(), normalizeLocusName(name), accessionNumber, version,
                    DNATools.createDNA(sequence.getSequence()), seqVersion);

            simpleRichSequence.setCircular(getCircular());
            if (getDescription() != null && !getDescription().isEmpty()) {
                simpleRichSequence.setDescription(getDescription());
            }

            if (getDivision() != null && !getDivision().isEmpty()) {
                simpleRichSequence.setDivision(getDivision());
            }

            if (getIdentifier() != null && !getIdentifier().isEmpty()) {
                simpleRichSequence.setIdentifier(getIdentifier());
            }

            if (sequence.getFeatures() != null && sequence.getFeatures().size() > 0) {
                Set<Feature> featureSet = new LinkedHashSet<>();

                for (DNAFeature sequenceFeature : sequence.getFeatures()) {
                    if (sequenceFeature == null) {
                        continue;
                    }

                    RichFeature.Template featureTemplate = new RichFeature.Template();
                    featureTemplate.annotation = getAnnotations(sequenceFeature);

                    List<DNAFeatureLocation> locations = sequenceFeature.getLocations();
                    if (locations == null || locations.size() == 0)
                        continue;

                    if (locations.size() == 1) {
                        DNAFeatureLocation location = locations.get(0);
                        featureTemplate.location = new SimpleRichLocation(new SimplePosition(
                                location.getGenbankStart()), new SimplePosition(location.getEnd()), 1, getStrand(sequenceFeature));
                    } else {
                        ArrayList<Location> members = new ArrayList<>();
                        for (DNAFeatureLocation location : locations) {
                            members.add(new SimpleRichLocation(new SimplePosition(location.getGenbankStart()),
                                    new SimplePosition(location.getEnd()), 1, getStrand(sequenceFeature)));
                        }
                        featureTemplate.location = new CompoundRichLocation(members);
                    }

                    featureTemplate.source = getDefaultFeatureSource();
                    featureTemplate.type = getFeatureType(sequenceFeature);
                    featureTemplate.rankedCrossRefs = new TreeSet<>();

                    SimpleRichFeature simpleRichFeature = new SimpleRichFeature(simpleRichSequence, featureTemplate);
                    featureSet.add(simpleRichFeature);
                }

                simpleRichSequence.setFeatureSet(featureSet);
            }
        } catch (Exception e) {
            throw new FormatterException("Failed to create generate genbank file", e);
        }

        RichSequence.IOTools.writeGenbank(outputStream, simpleRichSequence, getNamespace());
    }

    /**
     * Get the strand of the {@link DNAFeature} feature.
     *
     * @param sequenceFeature
     * @return Strand of the feature.
     */
    protected Strand getStrand(DNAFeature sequenceFeature) {
        Strand strand;

        if (sequenceFeature.getStrand() == -1) {
            strand = Strand.NEGATIVE_STRAND;
        } else if (sequenceFeature.getStrand() == 1) {
            strand = Strand.POSITIVE_STRAND;
        } else {
            strand = Strand.UNKNOWN_STRAND;
        }

        return strand;
    }

    /**
     * Convert {@link DNAFeature} into a {@link RichAnnotation}.
     *
     * @param sequenceFeature
     * @return RichAnnotation object.
     */
    protected RichAnnotation getAnnotations(DNAFeature sequenceFeature) {
        RichAnnotation richAnnotation = new SimpleRichAnnotation();

        if (sequenceFeature.getName() != null && !sequenceFeature.getName().isEmpty()) {
            richAnnotation
                    .addNote(new SimpleNote(RichObjectFactory.getDefaultOntology().getOrCreateTerm(
                            "label"), normalizeFeatureValue(sequenceFeature.getName()), 1));
        }

        int i = 0;
        for (DNAFeatureNote attribute : sequenceFeature.getNotes()) {
            String key = attribute.getName();
            String value = attribute.getValue();
            if (key == null || key.isEmpty() || key.toLowerCase().equals("label")) { // skip invalid or feature with "label" note
                continue;
            }

            richAnnotation.addNote(new SimpleNote(RichObjectFactory.getDefaultOntology()
                    .getOrCreateTerm(key), normalizeFeatureValue(value), i + 2));
            i++;
        }

        return richAnnotation;
    }

    /**
     * Retrieve feature type from given {@link DNAFeature}. Populate it with "misc_feature" if
     * undefined.
     *
     * @param sequenceFeature
     * @return Genbank Feature type.
     */
    protected String getFeatureType(DNAFeature sequenceFeature) {
        String featureType;

        if (sequenceFeature.getType() == null || sequenceFeature.getType().isEmpty()) {
            Logger.warn("SequenceFeature by id: " + sequenceFeature.getId()
                    + " has invalid genbank type.");

            featureType = "misc_feature";
        } else {
            featureType = sequenceFeature.getType();
        }

        return featureType;
    }

    /**
     * Return the default feature source for a Genbank file.
     *
     * @return Returns "org.jbei".
     */
    protected String getDefaultFeatureSource() {
        return "org.jbei";
    }

    /**
     * Truncate Locus Name to 10 characters, as per Genbank specification.
     *
     * @param locusName
     * @return
     */
    private String normalizeLocusName(String locusName) {
        if (locusName == null || locusName.isEmpty()) {
            return "";
        }

        /* Locus name has to be max 10 characters long */
        String result = locusName;

        if (locusName.length() > 10) {
            result = locusName.substring(0, 10);
        }

        return result;
    }

    /**
     * Clean up feature values by removing double quotes and whitespace.
     *
     * @param value
     * @return
     */
    private String normalizeFeatureValue(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }

        String result = value.trim();
        while (true) {
            if (result.length() > 2) {
                if (result.charAt(0) == '"' || result.charAt(result.length() - 1) == '"') {
                    result = result.substring(1, result.length() - 1);

                    result = result.trim();
                } else {
                    break;
                }
            } else {
                break;
            }
        }

        return result;
    }
}