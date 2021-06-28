package org.jbei.wors.lib.index;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.jbei.wors.lib.common.logging.Logger;
import org.jbei.wors.lib.dto.entry.PartData;
import org.jbei.wors.lib.part.PartSource;
import org.jbei.wors.lib.search.blast.Constants;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class DocumentIndex implements Closeable {

    private final IndexWriter writer;

    DocumentIndex(boolean createNew) throws IOException {
        writer = getIndexWriter(createNew);
    }

    private IndexWriter getIndexWriter(boolean create) throws IOException {
        Path path = Paths.get(Constants.LUCENE_INDEX_FOLDER);
        if (!Files.exists(path)) {
            if (create)
                Files.createDirectories(path);
            else {
                String errMsg = "Invalid lucene index folder: \"" + path.toString() + "\"";
                Logger.error(errMsg);
                throw new IOException(errMsg);
            }
        }

        Directory dir = FSDirectory.open(path);
        Analyzer analyzer = new StandardAnalyzer();
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        if (create)
            config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        else
            config.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);

        return new IndexWriter(dir, config);
    }

    /**
     * Indexes a single document
     */
    public void add(PartData data, PartSource source) throws IOException {
        String url = source.getUrl();
        String name = source.getName();
        String id = StringUtils.isEmpty(source.getId()) ? data.getPartId() : source.getId();

        Document doc = new Document();

        // add part fields
        addToDocument(doc, IndexField.RECORD_ID, data.getRecordId());
        addToDocument(doc, IndexField.ID, Long.toString(data.getId()));
        addToDocument(doc, IndexField.TYPE, data.getType().getName());
        addToDocument(doc, IndexField.PART_ID, data.getPartId());
        addToDocument(doc, IndexField.NAME, data.getName());
        addToDocument(doc, IndexField.SUMMARY, data.getShortDescription());
        addToDocument(doc, IndexField.KEYWORDS, data.getKeywords());
        addToDocument(doc, IndexField.CREATED, Long.toString(data.getCreationTime()));

        doc.add(new LongPoint(IndexField.CREATED.toString() + "_long", data.getCreationTime()));
        doc.add(new StringField(IndexField.SOURCE_URL.toString(), url, Field.Store.YES));
        doc.add(new StringField(IndexField.SOURCE_NAME.toString(), name, Field.Store.YES));
        if (!StringUtils.isEmpty(id))
            doc.add(new StringField(IndexField.SOURCE_ID.toString(), id, Field.Store.YES));

        doc.add(new StringField(IndexField.HAS_SAMPLE.toString(), (data.isHasSample() ? "true" : "false"), Field.Store.YES));
        doc.add(new StringField(IndexField.HAS_SEQUENCE.toString(), (data.isHasSequence() ? "true" : "false"), Field.Store.YES));

        if (!StringUtils.isEmpty(data.getShortDescription()))
            doc.add(new TextField(IndexField.SUMMARY.toString() + "_token", data.getShortDescription().toLowerCase(), Field.Store.NO));

        if (!StringUtils.isEmpty(data.getName()))
            doc.add(new TextField(IndexField.NAME.toString() + "_token", data.getName().toLowerCase(), Field.Store.NO));

        writer.addDocument(doc);
    }

    private void addToDocument(Document document, IndexField field, String value) {
        if (value == null || value.trim().isEmpty())
            return;

        Field pathField = new StringField(field.toString(), value, Field.Store.YES);
        document.add(pathField);
    }

    @Override
    public void close() throws IOException {
        if (writer != null)
            writer.close();
    }
}
