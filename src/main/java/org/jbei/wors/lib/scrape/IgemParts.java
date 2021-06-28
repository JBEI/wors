package org.jbei.wors.lib.scrape;

import org.apache.commons.lang3.StringUtils;
import org.jbei.wors.lib.common.logging.Logger;
import org.jbei.wors.lib.dto.DNAFeature;
import org.jbei.wors.lib.dto.DNAFeatureLocation;
import org.jbei.wors.lib.dto.FeaturedDNASequence;
import org.jbei.wors.lib.dto.entry.EntryType;
import org.jbei.wors.lib.dto.entry.PartData;
import org.jbei.wors.lib.dto.entry.PartSequence;
import org.jbei.wors.lib.part.PartSource;
import org.jbei.wors.lib.part.PartsProducer;
import org.jbei.wors.lib.search.blast.Constants;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.UUID;
import java.util.stream.Stream;

public class IgemParts implements PartsProducer {

    private BufferedReader reader;
    private Iterator<String> iterator;
    private PartSequence nextSequence;

    public IgemParts() {
    }

    private void initialize() throws IOException {
        InputStream is = new URL(Constants.IGEM_ALL_PARTS_URL).openStream();
        reader = new BufferedReader(new InputStreamReader(is));
        Stream<String> stream = reader.lines();
        iterator = stream.iterator();
    }

    @Override
    public boolean hasNext() {
        if (iterator == null) {
            try {
                initialize();
            } catch (IOException e) {
                Logger.error(e);
                return false;
            }
        }

        while (iterator.hasNext()) {
            String next = iterator.next();
            if (!next.startsWith(">"))
                continue;

            int i = next.indexOf(' ');
            if (i == -1)
                continue;

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                //
            }

            String partId = next.substring(1, i);
            try {
                nextSequence = parseIgemPart(Constants.IGEM_XML_PART_URL_PREFIX + partId);
                if (nextSequence == null)
                    continue;


                return true;
            } catch (Exception e) {
                Logger.error(e);
            }
        }
        return false;
    }

    @Override
    public PartSequence next() {
        if (nextSequence == null)
            throw new IllegalStateException("next part not available");
        return nextSequence;
    }

    public void close() {
        try {
            if (reader != null)
                reader.close();
        } catch (IOException e) {
            Logger.error(e);
        }
    }

    public static PartSequence parseIgemPart(String url) throws Exception {
        Logger.info("Fetching part: " + url);
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        org.w3c.dom.Document doc = dBuilder.parse(url);
        doc.getDocumentElement().normalize();
        NodeList nList = doc.getElementsByTagName("rsbpml");
        if (nList.getLength() == 0)
            return null;

        // find part_list node
        Node partListNode = null;
        nList = nList.item(0).getChildNodes();
        for (int i = 0; i < nList.getLength(); i += 1) {
            Node nNode = nList.item(i);
            if (nNode.getNodeType() != Node.ELEMENT_NODE)
                continue;

            if ("part_list".equalsIgnoreCase(nNode.getNodeName())) {
                partListNode = nNode;
                break;
            }
        }

        if (partListNode == null)
            return null;

        nList = partListNode.getChildNodes();

        for (int temp = 0; temp < nList.getLength(); temp++) {
            Node nNode = nList.item(temp);
            if (nNode.getNodeType() != Node.ELEMENT_NODE)
                continue;

            PartSequence partSequence = getIgemPart(nNode);
            if (partSequence == null)
                continue;

            return partSequence;
        }
        return null;
    }

    private static PartSequence getIgemPart(Node node) {
        if (node == null || node.getChildNodes().getLength() == 0)
            return null;

        NodeList list = node.getChildNodes();
        PartData partData = new PartData(EntryType.PART);

        FeaturedDNASequence sequence = new FeaturedDNASequence();
        PartSource source = null;
        for (int i = 0; i < list.getLength(); i += 1) {
            Node nNode = list.item(i);
            if (nNode.getNodeType() != Node.ELEMENT_NODE)
                continue;

            Element element = (Element) nNode;
            switch (nNode.getNodeName()) {
                case "part_id":
                    partData.setPartId(element.getTextContent());
                    partData.setRecordId(UUID.randomUUID().toString());
                    break;

                case "part_name":
                    partData.setName(element.getTextContent());

                case "part_short_name":
                    partData.setAlias(element.getTextContent());
                    break;

                case "part_short_desc":
                    partData.setShortDescription(element.getTextContent());
                    break;

                case "part_entered":
                    try {
                        Date date = new SimpleDateFormat("yyyy-mm-dd").parse(element.getTextContent());
                        partData.setCreationTime(date.getTime());
                    } catch (ParseException e) {
                        Logger.error(e);
                    }
                    break;

                case "part_author":
                    partData.setCreator(element.getTextContent());
                    partData.setOwner(element.getTextContent());
                    break;

                case "sequences":
                    NodeList nodeList = ((Element) nNode).getElementsByTagName("seq_data");
                    if (nodeList.getLength() == 1) {
                        String sequenceDna = cleanSequence(nodeList.item(0).getTextContent());
                        sequence.setSequence(sequenceDna);
                        partData.setHasSequence(true);
                    }
                    break;

                case "features":
                    for (int fIndex = 0; fIndex < nNode.getChildNodes().getLength(); fIndex += 1) {
                        Node featureNode = nNode.getChildNodes().item(fIndex);
                        if (featureNode.getNodeType() != Node.ELEMENT_NODE)
                            continue;

                        // parse feature
                        DNAFeature feature = parseSequenceFeature(featureNode);
                        if (feature == null)
                            continue;

                        sequence.getFeatures().add(feature);
                    }
                    break;

                case "part_url":
                    if (source == null)
                        source = new PartSource(element.getTextContent(), "iGem Part Registry", null);
                    break;

                case "sample_status":
                    partData.setHasSample("In stock".equalsIgnoreCase(element.getTextContent()));
                    break;
            }
        }

        if (StringUtils.isEmpty(partData.getName()))
            partData.setName(partData.getAlias());
        partData.setHasSequence(StringUtils.isNotEmpty(sequence.getSequence()));
        PartSequence partSequence = new PartSequence(partData, sequence);
        partSequence.setPartSource(source);
        return partSequence;
    }

    private static DNAFeature parseSequenceFeature(Node featuresNode) {
        if (featuresNode == null)
            return null;

        DNAFeature feature = new DNAFeature();
        DNAFeatureLocation location = new DNAFeatureLocation();
        feature.getLocations().add(location);

        NodeList featureNodeList = featuresNode.getChildNodes();
        for (int i = 0; i < featureNodeList.getLength(); i += 1) {
            Node featureNode = featureNodeList.item(i);
            if (featureNode.getNodeType() != Node.ELEMENT_NODE)
                continue;

            Element featureElement = (Element) featureNode;
            switch (featureElement.getNodeName()) {
                case "id":
                    feature.setId(Long.decode(featureElement.getTextContent()));
                    break;

                case "title":
                    feature.setName(featureElement.getTextContent());
                    break;

                case "type":
                    feature.setType(featureElement.getTextContent());
                    break;

                case "direction":
                    feature.setStrand("forward".equals(featureElement.getTextContent()) ? 1 : -1);
                    break;

                case "startpos":
                    location.setGenbankStart(Integer.decode(featureElement.getTextContent()));
                    break;

                case "endpos":
                    location.setEnd(Integer.decode(featureElement.getTextContent()));
                    break;
            }
        }

        return feature;
    }

    private static String cleanSequence(String sequence) {
        return sequence.replaceAll("\n", "").replaceAll("\\s+", "");
    }
}
