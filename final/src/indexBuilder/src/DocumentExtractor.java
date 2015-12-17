import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

/**
 * Created by Jianhong Li on 12/14/15.
 */
public class DocumentExtractor {
  public String kwd;
  public String body;
  public String externalId;
  static XMLInputFactory factory = XMLInputFactory.newInstance();

  // extract field keywords and body from article
  public DocumentExtractor(String path) {
    externalId = path;

    File xmlFile = new File(path);
    StringBuilder bodyBuilder = new StringBuilder();
    StringBuilder kwdBuilder = new StringBuilder();
    XMLStreamReader reader = null;
    try {
      reader = factory.createXMLStreamReader(new FileInputStream(xmlFile));
      while (reader.hasNext()) {
        try {
          if (reader.getEventType() == XMLStreamConstants.START_ELEMENT) {
            String localName = reader.getLocalName();
            if (localName.equals("kwd")) {
              try {
                kwdBuilder.append(reader.getElementText()).append(" ");
              } catch (Exception ignored) {}
            } else if (localName.equals("p")) {
              // Body field is composed of p (paragraphs)
              try {
                bodyBuilder.append(reader.getElementText()).append("\n");
              } catch (Exception ignored) {}
            }
          }
          reader.next();
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      try {
        if (reader != null)
          reader.close();
      } catch (Exception ignored) {}
    }
    this.body = bodyBuilder.toString();
    this.kwd = kwdBuilder.toString();
  }
}
