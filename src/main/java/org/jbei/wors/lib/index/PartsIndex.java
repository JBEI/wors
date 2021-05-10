package org.jbei.wors.lib.index;

import org.jbei.wors.lib.common.logging.Logger;
import org.jbei.wors.lib.dto.entry.PartSequence;
import org.jbei.wors.lib.part.Parts;
import org.jbei.wors.lib.scrape.AddGeneParts;
import org.jbei.wors.lib.scrape.GenBankParts;
import org.jbei.wors.lib.scrape.IceParts;
import org.jbei.wors.lib.scrape.IgemParts;
import org.jbei.wors.lib.search.blast.BlastPlus;
import org.jbei.wors.lib.search.blast.Constants;

import java.io.Closeable;
import java.io.IOException;

// come up with a better name
public class PartsIndex implements Closeable {

    private final BlastPlus blastIndex;
    private final DocumentIndex index;
    private int maxPartsPerSource;

    public PartsIndex(boolean createNew) throws IOException {
        blastIndex = new BlastPlus(Constants.BLAST_DB_FOLDER);
        index = new DocumentIndex(createNew);
        try {
            this.maxPartsPerSource = Integer.decode(Constants.MAX_PARTS_PER_REGISTRY);
        } catch (NumberFormatException nfe) {
            this.maxPartsPerSource = 0;
            Logger.warn(nfe.toString());
        }
    }

    public void rebuildAll() {
        Parts parts = registerPartSources();
        int currentCount = 0;
        while (parts.hasNext()) {
            PartSequence partSequence = parts.next();
            if (partSequence == null)
                continue;

            if (maxPartsPerSource != 0 && ++currentCount == maxPartsPerSource) {
                parts.skipCurrentPartSource();
                currentCount = 0;
            }

            try {
                index.add(partSequence.getPart(), partSequence.getPartSource());
                blastIndex.writeSequenceToFasta(partSequence.getPart(), partSequence.getSequence());
            } catch (Exception e) {
                Logger.error(e);
            }
        }
    }

    /**
     * Sources of parts to index. Currently includes
     * <br>
     * <ul>
     * <li>AddGene</li>
     * <li>GenBank</li>
     * <li>Ice Parts</li>
     * <li>iGem</li>
     * </ul>
     *
     * @return parts list
     */
    private Parts registerPartSources() {
        Parts parts = new Parts();

        parts.registerSource(new AddGeneParts());
        try {
            parts.registerSource(new GenBankParts());
        } catch (IOException e) {
            Logger.error(e);
        }
        parts.registerSource(new IceParts());
        parts.registerSource(new IgemParts());
        return parts;
    }

    @Override
    public void close() throws IOException {
        index.close();
        blastIndex.close();
    }
}
