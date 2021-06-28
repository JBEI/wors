package org.jbei.wors.lib.scrape;

import org.apache.commons.lang3.StringUtils;
import org.jbei.wors.lib.common.logging.Logger;
import org.jbei.wors.lib.dto.FeaturedDNASequence;
import org.jbei.wors.lib.dto.entry.EntryType;
import org.jbei.wors.lib.dto.entry.PartData;
import org.jbei.wors.lib.dto.entry.PartSequence;
import org.jbei.wors.lib.parsers.genbank.GenBankParser;
import org.jbei.wors.lib.part.PartSource;
import org.jbei.wors.lib.part.PartsProducer;
import org.jbei.wors.lib.search.blast.Constants;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.GZIPInputStream;

/**
 * Iterator over a list of genbank parts that are contained in a <code>.gz</code>
 *
 * @author Hector Plahar
 */
public class GenBankParts implements PartsProducer {

    private final String registryName = "National Center for Biotech Information";
    private final List<PartSequence> data;
    private final Iterator<Path> iterator;
    private long basePairLimit;

    public GenBankParts() throws IOException {
        DirectoryStream<Path> zipFileStream = Files.newDirectoryStream(Paths.get(Constants.GENBANK_INPUT_DIR));
        iterator = zipFileStream.iterator();
        data = new ArrayList<>();

        try {
            basePairLimit = Long.decode(Constants.GENBANK_BP_LIMIT);
        } catch (NumberFormatException e) {
            Logger.error(e);
            basePairLimit = 0;
        }
    }

    @Override
    public boolean hasNext() {
        if (data.isEmpty()) {
            parseNextFile();
        }

        return !data.isEmpty();
    }

    @Override
    public PartSequence next() {
        if (data.isEmpty())
            throw new NoSuchElementException("No next element available");

        return data.remove(0);
    }

    private void parseNextFile() {
        Path zipFile = null;

        while (iterator.hasNext()) {
            zipFile = iterator.next();
            if (!zipFile.toString().endsWith("seq.gz")) {
                zipFile = null;
                continue;
            }
            break;
        }

        if (zipFile == null)
            return;

        Logger.info("Processing " + zipFile.toString());
        Path multiSequenceFilePath = null;
        try {
            multiSequenceFilePath = decompress(zipFile);
            Iterator<String> sequenceFileLines = Files.lines(multiSequenceFilePath).iterator();

            String genbank = null;

            while (sequenceFileLines.hasNext()) {
                String line = sequenceFileLines.next();
                if (line.startsWith("LOCUS")) {
                    if (includeSequence(line)) {
                        genbank = (line + "\n");
                    } else {
                        Logger.info("Skipping genbank: " + line);
                        genbank = "";
                    }
                    continue;
                }

                if (StringUtils.isNotEmpty(genbank))
                    genbank += (line + "\n");

                // check if at end of sequence
                if (line.startsWith("//")) {
                    if (StringUtils.isNotEmpty(genbank)) {
                        PartSequence next = processSequence(genbank);
                        if (next == null)
                            return;

                        if (next.getSequence() != null) {
                            String url = Constants.NCBI_SEARCH_URL + "/" + next.getSequence().getIdentifier() + "/?report=genbank";
                            next.setPartSource(new PartSource(url, registryName, null));
                        }

                        data.add(next);
                    }
                }
            }
        } catch (IOException e) {
            Logger.error(e);
        } finally {
            Logger.info("Done with " + zipFile.toString());
            try {
                if (multiSequenceFilePath != null)
                    Files.deleteIfExists(multiSequenceFilePath);
            } catch (IOException e) {
                Logger.error(e);
            }
        }
    }

    private Path decompress(Path zipPath) throws IOException {
        String fileName = zipPath.getName(zipPath.getNameCount() - 1).toString();
        fileName = fileName.substring(0, fileName.lastIndexOf("."));
        Path output = Paths.get(Constants.TMP_DIR, fileName);

        try (GZIPInputStream gis = new GZIPInputStream(new FileInputStream(zipPath.toString()));
             FileOutputStream fos = new FileOutputStream(output.toString())) {

            byte[] buffer = new byte[1024];
            int len;
            while ((len = gis.read(buffer)) != -1) {
                fos.write(buffer, 0, len);
            }
        }

        return output;
    }


    private boolean includeSequence(String locusLine) {
        if (basePairLimit == 0)
            return true;

        String[] split = locusLine.split("\\s+");
        for (int i = 0; i < split.length; i += 1) {
            String item = split[i].trim();
            if (!item.equalsIgnoreCase("bp"))
                continue;

            if (i > 0) {
                try {
                    return basePairLimit >= Long.decode(split[i - 1]);
                } catch (NumberFormatException e) {
                    Logger.error(e);
                    return true;
                }
            }
            break;
        }
        return true;
    }

    private PartSequence processSequence(String genbank) {
        GenBankParser parser = new GenBankParser();
        try {
            FeaturedDNASequence sequence = parser.parse(genbank);
            if (sequence == null)
                return null;

            PartData partData = new PartData(sequence.isCircular() ? EntryType.PLASMID : EntryType.PART);
            partData.setHasSequence(true);
            partData.setName(sequence.getName());
            partData.setPartId(sequence.getIdentifier());
            partData.setRecordId(UUID.randomUUID().toString());
            partData.setCreator(sequence.getAuthors());
            partData.setShortDescription(sequence.getDescription());

            try {
                Date date = new SimpleDateFormat("dd-MMM-yyyy").parse(sequence.getDate());
                partData.setCreationTime(date.getTime());
            } catch (Exception e) {
                //
            }

            return new PartSequence(partData, sequence);
        } catch (Exception e) {
            Logger.error(e);
            return null;
        }
    }
}
