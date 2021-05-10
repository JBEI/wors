package org.jbei.wors.lib.search.blast;

import com.opencsv.CSVReader;
import org.apache.commons.io.FileUtils;
import org.biojava.bio.seq.DNATools;
import org.biojava.bio.symbol.IllegalSymbolException;
import org.biojava.bio.symbol.SymbolList;
import org.jbei.wors.lib.common.logging.Logger;
import org.jbei.wors.lib.dto.FeaturedDNASequence;
import org.jbei.wors.lib.dto.entry.PartData;
import org.jbei.wors.lib.dto.search.BlastProgram;
import org.jbei.wors.lib.dto.search.BlastQuery;
import org.jbei.wors.lib.dto.search.SearchResult;
import org.jbei.wors.lib.index.SearchIndex;
import org.jbei.wors.lib.utils.Utils;

import java.io.*;
import java.nio.channels.FileLock;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Enables (command line) interaction with BLAST+
 * <p>
 * Current usage is for blast searches and auto-annotation support
 *
 * @author Hector Plahar
 */
public class BlastPlus implements Closeable {

    private final BufferedWriter writer;
    private final File lockFile;
    private final FileLock lock;
    private final String indexPath;

    public BlastPlus(String indexPath) throws IOException {
        Logger.info("Creating blast index at: " + indexPath);
        if (!Files.exists(Paths.get(indexPath))) {
            Files.createDirectory(Paths.get(indexPath));
        }

        lockFile = Paths.get(indexPath, Constants.LOCK_FILE_NAME).toFile();
        this.indexPath = indexPath;

        if (lockFile.exists()) {
            Logger.info("lock file exists: " + lockFile.toString());
            if (lockFile.lastModified() <= (System.currentTimeMillis() - (1000 * 60 * 60 * 24))) {
                if (!lockFile.delete()) {
                    throw new IOException("Could not delete lock file: " + lockFile.toString());
                }
            } else {
                throw new IOException("Existing lock file: " + lockFile.toString());
            }
        } else {
            Logger.info("creating new lock file: " + lockFile.toString());
            if (!lockFile.createNewFile())
                throw new IOException("Could not create lock file: " + lockFile.toString());
        }

        Path newFastaFile = Paths.get(indexPath, Constants.NEW_INDEX_FILE_NAME);
        Files.deleteIfExists(newFastaFile);
        writer = Files.newBufferedWriter(newFastaFile, Charset.defaultCharset(), StandardOpenOption.CREATE_NEW);

        FileOutputStream fos = new FileOutputStream(lockFile);
        lock = fos.getChannel().tryLock();
    }

    /**
     * Runs a blast query in the specified database folder
     * using the specified options
     *
     * @param query   wrapper around blast query including options such as blast type
     * @param options command line options for blast
     * @return results of the query run. An empty string is returned if the specified blast database does not exist
     * in the ice data directory
     * @throws BlastException on exception running blast on the command line
     */
    private static String runBlastQuery(BlastQuery query, String... options) throws BlastException {
        if (query.getBlastProgram() == null)
            query.setBlastProgram(BlastProgram.BLAST_N);

        try {
            Path commandPath = Paths.get(Constants.BLAST_INSTALL, query.getBlastProgram().getName());
            String blastDb = Paths.get(Constants.BLAST_DB_FOLDER, Constants.BLAST_DB_NAME).toString();
            if (!Files.exists(Paths.get(blastDb + ".nsq"))) {
                return "";
            }

            String[] blastCommand = new String[3 + options.length];
            blastCommand[0] = commandPath.toString();
            blastCommand[1] = "-db";
            blastCommand[2] = blastDb;
            System.arraycopy(options, 0, blastCommand, 3, options.length);

            Process process = Runtime.getRuntime().exec(blastCommand);
            ProcessResultReader reader = new ProcessResultReader(process.getInputStream());
            reader.start();
            BufferedWriter programInputWriter = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));

            programInputWriter.write(query.getSequence());
            programInputWriter.flush();
            programInputWriter.close();
            process.getOutputStream().close();

            // TODO this should go into the thread itself & have future wait on it
            final int exitValue = process.waitFor();
            switch (exitValue) {
                case 0:
                    return reader.toString();

                case 1:
                    Logger.error("Error in query sequence(s) or BLAST options");
                    break;

                case 2:
                    Logger.error("Error in BLAST database");
                    break;

                default:
                    Logger.error("Unknown exit value " + exitValue);
            }
            return null;
        } catch (Exception e) {
            Logger.error(e);
            throw new BlastException(e);
        }
    }

    /**
     * Run a blast query using the following output format options
     * <ul>
     * <li><code>stitle</code> - subject title</li>
     * <li><code>qstart</code> - query match start index</li>
     * <li><code>qend</code> - query match end index</li>
     * <li><code>sstart</code> - subject match start index</li>
     * <li><code>send</code></li>
     * <li><code>sstrand</code></li>
     * <li><code>evalue</code></li>
     * <li><code>bitscore</code></li>
     * <li><code>length</code> - alignment length</li>
     * <li><code>nident</code> - number of identical matches</li>
     * </ul>
     *
     * @param query wrapper around blast query
     * @return map of unique entry identifier (whose sequence was a subject) to the search result hit details
     * @throws BlastException
     */
    public static HashMap<String, SearchResult> runBlast(BlastQuery query) throws BlastException {
        String result = runBlastQuery(query, "-perc_identity", "95", "-outfmt",
                "10 stitle qstart qend sstart send sstrand evalue bitscore score length nident");
        if (result == null)
            throw new BlastException("Exception running blast");
        return processBlastOutput(result, query.getSequence().length());
    }

    /**
     * Parses a blast output that represents a single hit
     *
     * @param line blast output for hit
     * @return object wrapper around details of the hit
     */
    private static SearchResult parseBlastOutputLine(String[] line) {
        try {
            String recordId = line[0];
            SearchIndex index = new SearchIndex();
            SearchResult searchResult = index.getByRecordId(recordId);
            if (searchResult == null)
                return null;

            searchResult.seteValue(line[6]);
            searchResult.setScore(Float.valueOf(line[8].trim()));
            searchResult.setAlignment(line[10]);
            searchResult.setQueryLength(Integer.parseInt(line[9].trim()));
            searchResult.setNident(Integer.parseInt(line[10].trim()));
            return searchResult;
        } catch (Exception e) {
            Logger.error(e);
            return null;
        }
    }

    /**
     * Processes the result of a blast search
     *
     * @param blastOutput result output from running blast on the command line
     * @param queryLength length of query sequence
     * @return mapping of entryId to search result object containing information about the blast search for that particular hit
     */
    private static LinkedHashMap<String, SearchResult> processBlastOutput(String blastOutput, int queryLength) {
        LinkedHashMap<String, SearchResult> hashMap = new LinkedHashMap<>();

        try (CSVReader reader = new CSVReader(new StringReader(blastOutput))) {
            List<String[]> lines = reader.readAll();
            reader.close();

            for (String[] line : lines) {
                SearchResult info = parseBlastOutputLine(line);
                if (info == null)
                    continue;

                info.setQueryLength(queryLength);
                String idString = info.getEntryInfo().getRecordId();
                SearchResult currentResult = hashMap.get(idString);
                // if there is an existing record for same entry with a lower relative score then replace
                if (currentResult == null)
                    hashMap.put(idString, info);
            }
        } catch (IOException e) {
            Logger.error(e);
            return null;
        }

        return hashMap;
    }

    private void formatBlastDb() throws IOException {
        ArrayList<String> commands = new ArrayList<>();
        String makeBlastDbCmd = Constants.BLAST_INSTALL + File.separator + "makeblastdb";
        commands.add(makeBlastDbCmd);
        commands.add("-dbtype nucl");
        commands.add("-in");
        String fastaFile = indexPath + File.separator + "bigfastafile.new";
        commands.add(fastaFile);
        commands.add("-logfile");
        String logFile = indexPath + File.separator + Constants.BLAST_DB_NAME + ".log";
        commands.add(logFile);
        commands.add("-out");
        String out = indexPath + File.separator + Constants.BLAST_DB_NAME;
        commands.add(out);
//        commands.add("-title");
//        commands.add("ICE Blast DB");
        String commandString = Utils.join(" ", commands);
        Logger.info("makeblastdb: " + commandString);

        Runtime runTime = Runtime.getRuntime();

        Path blastDbDir = Paths.get(Constants.BLAST_INSTALL);

        Process process = runTime.exec(commandString, new String[0], blastDbDir.toFile());
        InputStream blastOutputStream = process.getInputStream();
        InputStream blastErrorStream = process.getErrorStream();
        try {
            process.waitFor();
        } catch (InterruptedException e) {
            throw new IOException(e);
        }
        String outputString = Utils.getString(blastOutputStream);
        blastOutputStream.close();
        Logger.debug("format output was: " + outputString);

        String errorString = Utils.getString(blastErrorStream);
        blastErrorStream.close();
        Logger.debug("format error was: " + errorString);
        process.destroy();

        if (errorString.length() > 0) {
            Logger.error(errorString);
            throw new IOException("Could not make blast db");
        }
    }

    public void writeSequenceToFasta(PartData partData, FeaturedDNASequence sequence) throws IOException {
        if (partData == null || sequence == null || sequence.getSequence() == null || sequence.getSequence().isEmpty())
            return;

        // get sequence from
        String sequenceString;
        SymbolList symL;
        try {
            symL = DNATools.createDNA(sequence.getSequence().trim());
            sequenceString = breakUpLines(symL.seqString() + symL.seqString());

        } catch (IllegalSymbolException e1) {
            Logger.error(e1);
            return;
        }

        sequenceString = sequenceString.replaceAll("\n", "");
        if (sequenceString.length() > 0) {
            writer.write(">" + partData.getRecordId() + "\n");
            writer.write(sequenceString + "\n");
            writer.flush();
        }
    }

    /**
     * Format into 6 column, 10 basepairs per column display.
     *
     * @param input sequence string.
     * @return Formatted sequence output.
     */
    public static String breakUpLines(String input) {
        StringBuilder result = new StringBuilder();

        int counter = 0;
        int index = 0;
        int end = input.length();
        while (index < end) {
            result = result.append(input.substring(index, index + 1));
            counter = counter + 1;
            index = index + 1;

            if (counter == 59) {
                result = result.append("\n");
                counter = 0;
            }
        }
        return result.toString();
    }

    @Override
    public void close() throws IOException {
        lock.close();
        writer.close();
        formatBlastDb();
        FileUtils.deleteQuietly(lockFile);
        FileUtils.deleteQuietly(Paths.get(indexPath, File.separator, "bigfastafile.new").toFile());
    }

    /**
     * Thread that reads the result of a command line process execution
     */
    static class ProcessResultReader extends Thread {
        final InputStream inputStream;
        final StringBuilder sb;

        ProcessResultReader(final InputStream is) {
            this.inputStream = is;
            this.sb = new StringBuilder();
        }

        public void run() {
            try (final InputStreamReader inputStreamReader = new InputStreamReader(inputStream)) {
                final BufferedReader br = new BufferedReader(inputStreamReader);
                String line;
                while ((line = br.readLine()) != null) {
                    this.sb.append(line).append("\n");
                }
            } catch (final IOException ioe) {
                Logger.error(ioe.getMessage());
                throw new RuntimeException(ioe);
            }
        }

        @Override
        public String toString() {
            return this.sb.toString();
        }
    }
}
