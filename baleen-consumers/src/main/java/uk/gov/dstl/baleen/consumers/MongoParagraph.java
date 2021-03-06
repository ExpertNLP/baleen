// Dstl (c) Crown Copyright 2019
package uk.gov.dstl.baleen.consumers;

import java.util.ArrayList;
import java.util.List;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.ExternalResource;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.DocumentAnnotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.bson.Document;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import uk.gov.dstl.baleen.consumers.utils.ConsumerUtils;
import uk.gov.dstl.baleen.resources.SharedMongoResource;
import uk.gov.dstl.baleen.types.language.Paragraph;
import uk.gov.dstl.baleen.uima.BaleenConsumer;

/**
 * Output paragraphs from a document as individual documents in Mongo
 *
 * @baleen.javadoc
 */
public class MongoParagraph extends BaleenConsumer {

  /**
   * Connection to Mongo
   *
   * @baleen.resource uk.gov.dstl.baleen.resources.SharedMongoResource
   */
  public static final String KEY_MONGO = "mongo";

  @ExternalResource(key = SharedMongoResource.RESOURCE_KEY)
  private SharedMongoResource mongoResource;

  /**
   * Should a hash of the content be used to generate the Document ID? If false, then a hash of the
   * Source URI is used instead.
   *
   * @baleen.config true
   */
  public static final String PARAM_CONTENT_HASH_AS_ID = "contentHashAsId";

  @ConfigurationParameter(name = PARAM_CONTENT_HASH_AS_ID, defaultValue = "true")
  private boolean contentHashAsId = true;

  /**
   * The collection to output paragraphs to
   *
   * @baleen.config paragraphs
   */
  public static final String PARAM_PARAGRAPHS_COLLECTION = "collection";

  @ConfigurationParameter(name = PARAM_PARAGRAPHS_COLLECTION, defaultValue = "paragraphs")
  private String paragraphsCollectionName;

  private MongoCollection<Document> paragraphsCollection;

  // Fields
  public static final String FIELD_DOCUMENT_ID = "docId";
  public static final String FIELD_CONTENT = "content";
  public static final String FIELD_DOCUMENT_SOURCE = "source";
  public static final String FIELD_BEGIN = "begin";
  public static final String FIELD_END = "end";

  /** Get the mongo db, collection and create some indexes */
  @Override
  public void doInitialize(UimaContext aContext) throws ResourceInitializationException {
    MongoDatabase db = mongoResource.getDB();
    paragraphsCollection = db.getCollection(paragraphsCollectionName);

    paragraphsCollection.createIndex(new Document(FIELD_DOCUMENT_ID, 1));
    paragraphsCollection.createIndex(new Document(FIELD_BEGIN, 1));
  }

  @Override
  public void doDestroy() {
    paragraphsCollection = null;
  }

  protected String getUniqueId(JCas jCas) {
    return ConsumerUtils.getExternalId(getDocumentAnnotation(jCas), contentHashAsId);
  }

  @Override
  protected void doProcess(JCas jCas) throws AnalysisEngineProcessException {
    String documentId = getUniqueId(jCas);

    List<Document> batchInsert = new ArrayList<>();

    for (Paragraph paragraph : JCasUtil.select(jCas, Paragraph.class)) {
      Document doc = new Document();

      DocumentAnnotation da = getDocumentAnnotation(jCas);

      doc.append(FIELD_DOCUMENT_ID, documentId)
          .append(FIELD_CONTENT, paragraph.getCoveredText())
          .append(FIELD_DOCUMENT_SOURCE, da.getSourceUri())
          .append(FIELD_BEGIN, paragraph.getBegin())
          .append(FIELD_END, paragraph.getEnd());

      batchInsert.add(doc);
    }

    if (!batchInsert.isEmpty()) paragraphsCollection.insertMany(batchInsert);
  }
}
