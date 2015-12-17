import java.io.IOException;
import java.io.File;

import org.apache.lucene.analysis.core.StopAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

/**
 *
 * @author Jianhong Li
 */
public class IndexBuilder {
  static FieldType fieldType = new FieldType();

  /** Creates a new instance of Indexer */
  public IndexBuilder() {
  }

  private IndexWriter indexWriter = null;

  public IndexWriter getIndexWriter(boolean create, String outputdir)
      throws IOException {
    if (indexWriter != null)
      return indexWriter;
    Directory indexDir = FSDirectory.open(new File(outputdir));
    IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_43,
        new StandardAnalyzer(Version.LUCENE_43));
    indexWriter = new IndexWriter(indexDir, config);
    return indexWriter;
  }

  public void closeIndexWriter() throws IOException {
    if (indexWriter != null) {
      indexWriter.close();
    }
  }

  /**
   * Create index for a document
   * @param path the path of the document
   * @throws IOException
   */
  public void buildDocumentIndex(String path)
      throws IOException {
    System.out.println("Create index for " + path);
    DocumentExtractor document = new DocumentExtractor(path);
    if (document.body == null || document.body.equals(""))
      return;
    IndexWriter writer = getIndexWriter(false, "");
    Document doc = new Document();
    doc.add(new StringField("externalId", document.externalId, Field.Store.YES));
    doc.add(new Field("body", document.body, fieldType));
    doc.add(new Field("keywords", document.kwd, fieldType));
    String fullSearchableText = document.body + " " + document.kwd;
    doc.add(new TextField("content", fullSearchableText, Field.Store.NO));
    StopAnalyzer stopAnalyzer = new StopAnalyzer(Version.LUCENE_43);
    writer.addDocument(doc, stopAnalyzer);
  }

  /**
   * Helper function to recursive traverse directory
   * @param node the file node to traverse
   * @throws IOException
   */
  private void buildDirectoryIndex(File node) throws IOException {
    if (node.isDirectory()) {
      String[] subNote = node.list();
      for (String filename : subNote) {
        buildDirectoryIndex(new File(node, filename));
      }
    } else {
      buildDocumentIndex(node.getAbsolutePath());
    }
  }

  /**
   * Create Lucene index of a set of documents
   * @param input the directory containing documents to be indexed
   * @param output generated index
   * @throws IOException
   */
  public void buildIndex(String input, String output) throws IOException {
    fieldType.setStoreTermVectors(true);
    fieldType.setStoreTermVectorPositions(true);
    fieldType.setStoreTermVectorOffsets(true);
    fieldType.setTokenized(true);
    fieldType.setStored(true);
    fieldType.setIndexed(true);

    // Erase existing index
    getIndexWriter(true, output);

    // Index all Accommodation entries
    buildDirectoryIndex(new File(input));

    // Don't forget to close the index writer when done
    closeIndexWriter();
  }

}
