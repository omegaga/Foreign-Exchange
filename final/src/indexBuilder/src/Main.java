import java.io.IOException;

/**
 * Created by Jianhong Li on 12/15/15.
 */
public class Main {
  static public void main(String[] args) throws IOException{
    IndexBuilder indexer = new IndexBuilder();
    if (args.length != 2) {
      System.err.println("Usage: java IndexBuilder <document path> <index path>");
      System.exit(-1);
    }
    indexer.buildIndex(args[0], args[1]);
  }
}
