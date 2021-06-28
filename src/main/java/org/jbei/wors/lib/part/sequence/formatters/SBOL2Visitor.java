package org.jbei.wors.lib.part.sequence.formatters;

import org.apache.commons.lang3.StringUtils;
import org.jbei.wors.lib.dto.DNAFeature;
import org.jbei.wors.lib.dto.DNAFeatureLocation;
import org.jbei.wors.lib.dto.FeaturedDNASequence;
import org.jbei.wors.lib.dto.entry.PartData;
import org.jbei.wors.lib.dto.entry.PartSequence;
import org.sbolstandard.core2.*;

import javax.xml.namespace.QName;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class SBOL2Visitor {

    static final String ICE_NS = "http://ice.jbei.org#";
    static final String ICE_PREFIX = "ice";

    private ComponentDefinition componentDefinition;
    private String uriString;
    private SBOLDocument doc;
    private int annotationCount;

    public SBOL2Visitor(SBOLDocument doc) throws SBOLValidationException, URISyntaxException {
        this.doc = doc;
        uriString = "https://www.webofregistries.org/search/entry";
        doc.addNamespace(new URI(ICE_NS), ICE_PREFIX);
    }

    public void visit(PartSequence partSequence) throws SBOLValidationException {
        // ice data model conflates the sequence and component
        PartData entry = partSequence.getPart();
        FeaturedDNASequence sequence = partSequence.getSequence();

        // Set required properties
        String partId = entry.getPartId();
        componentDefinition = doc.createComponentDefinition(uriString, partId, "1", ComponentDefinition.DNA);
        componentDefinition.setName(entry.getName());
        componentDefinition.setDescription(entry.getShortDescription());

        String dsUri = "sequence_" + entry.getRecordId().replaceAll("[\\s\\-()]", "");
        org.sbolstandard.core2.Sequence dnaSequence = doc.createSequence(
                uriString, dsUri, "1", sequence.getSequence(), org.sbolstandard.core2.Sequence.IUPAC_DNA);

        dnaSequence.setElements(sequence.getSequence());
        componentDefinition.addSequence(dnaSequence);
        List<DNAFeature> features = new ArrayList<>(sequence.getFeatures());
        for (DNAFeature feature : features) {
            visit(feature);
        }

        componentDefinition.createAnnotation(new QName(ICE_NS, "id", ICE_PREFIX), entry.getId());

        if (entry.getRecordId() != null)
            componentDefinition.createAnnotation(new QName(ICE_NS, "recordId", ICE_PREFIX), entry.getRecordId());

        if (entry.getType() != null)
            componentDefinition.createAnnotation(new QName(ICE_NS, "type", ICE_PREFIX), entry.getType().getName());

        if (entry.getOwner() != null)
            componentDefinition.createAnnotation(new QName(ICE_NS, "owner", ICE_PREFIX), entry.getOwner());

        if (entry.getOwnerEmail() != null)
            componentDefinition.createAnnotation(new QName(ICE_NS, "ownerEmail", ICE_PREFIX), entry.getOwnerEmail());

        if (entry.getCreator() != null)
            componentDefinition.createAnnotation(new QName(ICE_NS, "creator", ICE_PREFIX), entry.getCreator());

        if (entry.getCreatorEmail() != null)
            componentDefinition.createAnnotation(new QName(ICE_NS, "creatorEmail", ICE_PREFIX), entry.getCreatorEmail());

        if (entry.getStatus() != null)
            componentDefinition.createAnnotation(new QName(ICE_NS, "status", ICE_PREFIX), entry.getStatus());

        if (entry.getAlias() != null)
            componentDefinition.createAnnotation(new QName(ICE_NS, "alias", ICE_PREFIX), entry.getAlias());

        for (String selectionMarker : entry.getSelectionMarkers()) {
            componentDefinition.createAnnotation(new QName(ICE_NS, "selectionMarker", ICE_PREFIX), selectionMarker);
        }

        if (entry.getLinks() != null) {
            int i = 1;
            for (String link : entry.getLinks()) {
                if (StringUtils.isNotEmpty(link) && StringUtils.isNotEmpty(link))
                    componentDefinition.createAnnotation(new QName(ICE_NS, "link_" + i++, ICE_PREFIX), link);
            }
        }

        if (entry.getKeywords() != null)
            componentDefinition.createAnnotation(new QName(ICE_NS, "keywords", ICE_PREFIX), entry.getKeywords());

        if (entry.getShortDescription() != null)
            componentDefinition.createAnnotation(new QName(ICE_NS, "shortDescription", ICE_PREFIX), entry.getShortDescription());

        if (entry.getLongDescription() != null)
            componentDefinition.createAnnotation(new QName(ICE_NS, "longDescription", ICE_PREFIX), entry.getLongDescription());

        if (entry.getReferences() != null)
            componentDefinition.createAnnotation(new QName(ICE_NS, "references", ICE_PREFIX), entry.getReferences());

        if (entry.getCreationTime() != 0)
            componentDefinition.createAnnotation(new QName(ICE_NS, "creationTime", ICE_PREFIX), new Date(entry.getCreationTime()).toString());

        if (entry.getBioSafetyLevel() != null)
            componentDefinition.createAnnotation(new QName(ICE_NS, "bioSafetyLevel", ICE_PREFIX), entry.getBioSafetyLevel());

        if (entry.getIntellectualProperty() != null)
            componentDefinition.createAnnotation(new QName(ICE_NS, "intellectualProperty", ICE_PREFIX), entry.getIntellectualProperty());

        if (entry.getFundingSource() != null)
            componentDefinition.createAnnotation(new QName(ICE_NS, "fundingSource", ICE_PREFIX), entry.getFundingSource());

        if (entry.getPrincipalInvestigator() != null)
            componentDefinition.createAnnotation(new QName(ICE_NS, "principalInvestigator", ICE_PREFIX), entry.getPrincipalInvestigator());

        // TODO: samples
        // TODO: attachments
    }

    public void visit(DNAFeature feature) throws SBOLValidationException {
        annotationCount++;

        if (feature.getLocations() != null && !feature.getLocations().isEmpty()) {
            DNAFeatureLocation location = (DNAFeatureLocation) feature.getLocations().toArray()[0];
            SequenceAnnotation annotation;
            OrientationType orientation = feature.getStrand() == 1 ? OrientationType.INLINE : OrientationType.REVERSECOMPLEMENT;

            if (location.getEnd() < location.getGenbankStart()) {
                annotation = componentDefinition.createSequenceAnnotation(
                        "annotation" + annotationCount, "locationStart", location.getGenbankStart(),
                        feature.getSequence().length(),
                        orientation
                );
                annotation.addRange("locationEnd" + annotationCount, 1, location.getEnd(), orientation);
            } else {
                annotation = componentDefinition.createSequenceAnnotation(
                        "annotation" + annotationCount,
                        "location",
                        location.getGenbankStart(), location.getEnd(),
                        orientation);
            }

            annotation.addRole(IceSequenceOntology.getURI(feature.getType()));
            annotation.setName(feature.getName());
        }
    }
}