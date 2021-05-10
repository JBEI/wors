package org.jbei.wors.lib.scrape;

import org.apache.commons.lang3.StringUtils;
import org.jbei.wors.lib.common.logging.Logger;
import org.jbei.wors.lib.dto.FeaturedDNASequence;
import org.jbei.wors.lib.dto.entry.EntryType;
import org.jbei.wors.lib.dto.entry.PartData;
import org.jbei.wors.lib.dto.entry.PartSequence;
import org.jbei.wors.lib.dto.entry.PlasmidData;
import org.jbei.wors.lib.parsers.genbank.GenBankParser;
import org.jbei.wors.lib.part.PartSource;
import org.jbei.wors.lib.part.PartsProducer;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AddGeneParts implements PartsProducer {

    private int pageNumber = 1;
    private boolean hasNextCalled = false;
    private final List<PartSequence> results;

    /**
     * constructor. init
     */
    public AddGeneParts() {
        results = new ArrayList<>();
//        retrieveAddGeneParts();
    }

    private FeaturedDNASequence parseRemoteSequence(String url) throws IOException {
        Connection.Response response = Jsoup.connect(url)
                //.followRedirects(true)        // follow redirects (it's the default)
                .ignoreContentType(true)        // accept not just HTML
                .maxBodySize(10 * 1000 * 1000)  // accept 10M bytes (default is 1M), or set to 0 for unlimited
                .execute();                     // send GET request

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(response.bodyAsBytes());
        out.close();

        // parse sequence
        return new GenBankParser().parse(new String(out.toByteArray()));
    }

    private void setFullSequence(PartSequence plasmid, List<Node> nodes) throws IOException {
        for (Node node : nodes) {
            if (node.childNodeSize() == 0) // or is instance of TextNode
                continue;

            if (!"genbank".equalsIgnoreCase(((Element) node).text()))
                continue;

            List<Node> childNodes = node.childNodes();
            for (Node childNode : childNodes) {
                String url = childNode.attr("href");
                if (StringUtils.isEmpty(url))
                    continue;

                // download file and set to the main part
                FeaturedDNASequence sequence = parseRemoteSequence(url);
                if (sequence != null) {
                    plasmid.setSequence(sequence);

                    if (!StringUtils.isEmpty(sequence.getDate())) {
                        try {
                            plasmid.getPart().setCreationTime(new SimpleDateFormat("dd-MMM-yyyy").parse(sequence.getDate()).getTime());
                        } catch (Exception e) {
                            Logger.error(e);
                        }
                    }
                }
                return;
            }
        }
    }

    private void getSequenceInformation(String id, PartSequence plasmid, Element element) {
        int nodeSize = element.select("div#sequence_information").get(0).childNodeSize();
        if (nodeSize > 1) {
            // get sequences
            try {
                Document doc = Jsoup.connect("https://www.addgene.org/" + id.trim() + "/sequences").get();

                // get full sequence (if available)
                Elements fullSequenceElement = doc.select("section#depositor-full ul.addgene-nav-list");
                if (fullSequenceElement.isEmpty()) {

                    // get add gene element
                    fullSequenceElement = doc.select("section#addgene-full ul.addgene-nav-list");
                    if (!fullSequenceElement.isEmpty()) {
                        setFullSequence(plasmid, fullSequenceElement.get(0).childNodes());
                    }
                }

                // see if there are any partial sequences
                Elements elements = doc.select("section#addgene-partial ul.addgene-nav-list");
                for (Element partialElement : elements) {
                    List<Node> partials = partialElement.childNodes();

                    for (Node node : partials) {
                        if (node.childNodeSize() == 0) // or is instance of TextNode
                            continue;

                        if ("genbank".equalsIgnoreCase(((Element) node).text())) {
                            List<Node> childNodes = node.childNodes();
                            for (Node childNode : childNodes) {
                                String url = childNode.attr("href");
                                if (StringUtils.isEmpty(url))
                                    continue;

                                // download file for each child
                                FeaturedDNASequence sequence = parseRemoteSequence(url);

                                // set children
                                if (sequence != null) {
                                    PartData child = new PartData(EntryType.PART);
                                    child.setRecordId(UUID.randomUUID().toString());

                                    if (!StringUtils.isEmpty(sequence.getDate())) {
                                        try {
                                            child.setCreationTime(new SimpleDateFormat("dd-MMM-yyyy").parse(sequence.getDate()).getTime());
                                        } catch (Exception e) {
                                            Logger.error(e);
                                        }
                                    }

                                    String description = sequence.getDescription().replaceAll("\r\n", "");
                                    sequence.setDescription(description);
                                    child.setShortDescription(description);
                                    child.setName(sequence.getName());

                                    PartSequence childSequence = new PartSequence();
                                    childSequence.setPart(child);
                                    childSequence.setSequence(sequence);
                                    plasmid.getChildren().add(childSequence);
                                }
                            }
                            break;
                        }
                    }
                }
            } catch (IOException e) {
                Logger.error(e);
            }
        }
    }

    private void retrieveAddGeneParts() {
        results.clear();

        try {
            int resultsPerPage = 10;
            String url = "https://www.addgene.org/search/advanced/?q=&results_per_page=" + resultsPerPage + "&page="
                    + pageNumber;

            // for page of results
            Document document = Jsoup.connect(url).get();
            Elements elements = document.select("section#plasmid-results div.col-xs-3:contains(#)");

            for (Element element : elements) {
                List<Node> children = element.childNodes();
                if (children.isEmpty())
                    continue;

                // fetch the plasmid number
                TextNode node = (TextNode) children.get(0);
                // expected format is "#number"
                String[] split = node.text().split("#");
                if (split.length != 2)
                    continue;

                String plasmidNumber = split[1];

                // retrieve the plasmid data using the plasmid number and index
                PartSequence sequence = retrievePlasmid(plasmidNumber.trim());
                results.add(sequence);
            }
            pageNumber += 1;
        } catch (Exception e) {
            Logger.error(e);
        }
    }

    public PartSequence retrievePlasmid(String plasmidId) {
        try {
            plasmidId = plasmidId.trim();
            PartData partData = new PartData(EntryType.PLASMID);
            partData.setPartId(plasmidId);
            partData.setRecordId(UUID.randomUUID().toString());
            partData.setHasSample(true);
            PartSequence partSequence = new PartSequence();
            final String partUrl = "https://www.addgene.org/" + plasmidId.trim() + "/";
            String sourceName = "AddGene Plasmid Repository";
            PartSource partSource = new PartSource(partUrl, sourceName, plasmidId);
            partSequence.setPartSource(partSource);
            partSequence.setPart(partData);

            // get the document page for the plasmid
            Document document = Jsoup.connect(partUrl).get();

            // plasmid description section
            Elements ulElements = document.select("div#top-section div#description-container ul#plasmid-description-list div.field");

            for (Element element : ulElements) {
                if (element.childNodes().isEmpty())
                    continue;

                String label = element.select("div.field-label").get(0).text().trim();
                switch (label.toLowerCase()) {
                    case "sequence information":
                        getSequenceInformation(plasmidId, partSequence, element);
                        break;

                    case "purpose": {
                        String value = element.select("div.field-content").get(0).text().trim();
                        partData.setShortDescription(value);
                        break;
                    }

                    case "depositing lab": {
                        String value = element.select("div.field-content").get(0).text().trim();
                        partData.setPrincipalInvestigator(value);
                        break;
                    }
                }
            }

            // name
            String name = ((TextNode) document.select("div.item-main-col span.material-name").get(0).childNodes().get(0)).text();
            partData.setName(name);

            // part details
            Elements detailElements = document.select("div#detail-sections div.col-xs-6 section");
            for (Element element : detailElements) {
                String title = element.select("h2 span.title").get(0).text();

                // backbone  section
                if ("backbone".equalsIgnoreCase(title)) {
                    Elements fieldElements = element.select("li.field");
                    for (Element fieldElement : fieldElements) {
                        String label = fieldElement.select("div.field-label").text();

                        // check backbone
                        if ("vector backbone".equalsIgnoreCase(label)) {
                            String backbone = fieldElement.childNodes().get(2).toString();
                            if (partData.getPlasmidData() == null) {
                                partData.setPlasmidData(new PlasmidData());
                            }
                            partData.getPlasmidData().setBackbone(backbone);
                        }

                        // check selection marker
                        if ("selectable markers".equalsIgnoreCase(label)) {
                            String markers = fieldElement.childNodes().get(2).toString();
                            partData.getSelectionMarkers().add(markers);
                        }
                    }
                }

                if ("growth in bacteria".equalsIgnoreCase(title)) {
                    Elements fieldElements = element.select("li.field");
                    for (Element fieldElement : fieldElements) {
                        String label = fieldElement.select("div.field-label").text();

                        // bacterial resistance
                        if (label.startsWith("Bacterial Resistance")) {
                            partData.getSelectionMarkers().add(fieldElement.childNodes().get(2).toString().trim());
                        }
                    }
                }
            }

            // references
            String references = document.select("div.addgene-item-main-row section#how-to-cite div.indent").text();
            partData.setReferences(references);
            return partSequence;
        } catch (Exception e) {
            Logger.error("Exception indexing addgene " + plasmidId, e);
            return null;
        }
    }

    @Override
    public boolean hasNext() {
        hasNextCalled = true;
        if (results.isEmpty()) {
            retrieveAddGeneParts();
        }

        return !results.isEmpty();
    }

    @Override
    public PartSequence next() {
        if (!hasNextCalled)
            throw new IllegalStateException("hasNext not called");
        hasNextCalled = false;
        return results.remove(0);
    }
}
