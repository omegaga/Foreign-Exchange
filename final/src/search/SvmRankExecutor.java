import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Created by Jianhong Li on 11/26/15.
 */
public class SvmRankExecutor {
  /**
   * runs svm_rank_learn from within Java to train the model
   * @param execPath The location of the svm_rank_learn utility
   * @param featGenC The value of letor:c
   * @param qrelsFeatureOutputFile The file containing features of relevant documents
   * @param modelOutputFile The file to output the model
   * @throws Exception
   */
  public static void learn(String execPath,
                           String featGenC,
                           String qrelsFeatureOutputFile,
                           String modelOutputFile)
      throws Exception {
    Process cmdProc = Runtime.getRuntime().exec(
        new String[] {
            execPath,
            "-c",
            featGenC,
            qrelsFeatureOutputFile,
            modelOutputFile});

    call(cmdProc);
  }

  public static void classify(String execPath,
                              String qrelsFeatureOutputFile,
                              String modelFile,
                              String predictionFile)
      throws Exception {

    Process cmdProc = Runtime.getRuntime().exec(
        new String[] {
            execPath,
            qrelsFeatureOutputFile,
            modelFile,
            predictionFile});

    call(cmdProc);
  }

  private static void call(Process cmdProc) throws Exception {
    // The stdout/stderr consuming code MUST be included.
    // It prevents the OS from running out of output buffer space and stalling.

    // consume stdout and print it out for debugging purposes
    BufferedReader stdoutReader = new BufferedReader(
        new InputStreamReader(cmdProc.getInputStream()));
    String line;
    while ((line = stdoutReader.readLine()) != null) {
      System.out.println(line);
    }
    // consume stderr and print it for debugging purposes
    BufferedReader stderrReader = new BufferedReader(
        new InputStreamReader(cmdProc.getErrorStream()));
    while ((line = stderrReader.readLine()) != null) {
      System.out.println(line);
    }

    // get the return value from the executable. 0 means success, non-zero
    // indicates a problem
    int retValue = cmdProc.waitFor();
    if (retValue != 0) {
      throw new Exception("SVM Rank crashed.");
    }
  }
}
