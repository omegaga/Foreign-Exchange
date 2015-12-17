/**
 *  Copyright (c) 2015, Carnegie Mellon University.  All Rights Reserved.
 */
import java.io.*;
import java.util.*;

/**
 *  The NEAR operator for all retrieval models.
 */
public class QryIopNear extends QryIop {

  private int distance = 0;

  /**
   *  NEAR operator constructor with distance
   *  @param distance A distance argument.
   */
  public QryIopNear(int distance) {
    this.distance = distance;
  }
  /**
   *  Evaluate the query operator; the result is an internal inverted
   *  list that may be accessed via the internal iterators.
   *  @throws IOException Error accessing the Lucene index.
   */
  protected void evaluate () throws IOException {

    //  Create an empty inverted list.  If there are no query arguments,
    //  that's the final result.

    this.invertedList = new InvList (this.getField());

    if (args.size () == 0) {
      return;
    }

    //  Each pass of the loop adds 1 document to result inverted list
    //  until all of the argument inverted lists are depleted.

    boolean exhausted = false;

    while (true) {

      boolean matchFound = false;
      int docid = Qry.INVALID_DOCID;

      // Keep trying until a match is found or no match is possible.

      while (! matchFound) {

        // Get the docid of the first query argument.

        Qry q_0 = this.args.get (0);


        if (! q_0.docIteratorHasMatch (null)) {
          exhausted = true;
          break;
        }

        int docid_0 = q_0.docIteratorGetMatch ();

        // Other query arguments must match the docid of the first query
        // argument.

        matchFound = true;

        for (int i=1; i<this.args.size(); i++) {
          Qry q_i = this.args.get(i);

          q_i.docIteratorAdvanceTo (docid_0);

          if (! q_i.docIteratorHasMatch (null)) {	// If any argument is exhausted
            exhausted = true;
            break;
          }

          if (exhausted)
            break;
          int docid_i = q_i.docIteratorGetMatch ();

          if (docid_0 != docid_i) {	// docid_0 can't match.  Try again.
            q_0.docIteratorAdvanceTo (docid_i);
            matchFound = false;
            break;
          }
        }

        if (matchFound) {
          docid = docid_0;
        }
      }

      if (exhausted)
        break;

      //  Create a new posting that satisfies the requirement of NEAR operator by
      //  iterating through the positions and trying to build a term list that
      //  distance of positions of two adjacent element is no more than n.

      List<Integer> positions = new ArrayList<Integer>();

      QryIop q_0 = (QryIop) this.args.get(0);
      while (q_0.locIteratorHasMatch()) {
        boolean success = true;
        int locid_last = q_0.locIteratorGetMatch(); // continue to find a list

        for (int i = 1; i < this.args.size(); ++i) {
          QryIop q_i = (QryIop) this.args.get(i);

          q_i.locIteratorAdvancePast(locid_last);
          if (! q_i.locIteratorHasMatch()) { // If any term is exhausted
            success = false;
            break;
          }
          int locid_now = q_i.locIteratorGetMatch();
          if (locid_now - locid_last > this.distance) { // break if fail to find a list
            success = false;
            break;
          }
          q_i.locIteratorAdvancePast(locid_now);
          locid_last = locid_now;
        }
        if (success) {
          // Add the position of last document to the new position list
          positions.add(locid_last);
        }
        // the first query argument can go advance
        q_0.locIteratorAdvance();
      }

      if (positions.size() > 0) {
        // Sort positions and add it to inverted list
        Collections.sort (positions);
        this.invertedList.appendPosting(docid, positions);
      }

      q_0.docIteratorAdvancePast(docid);
    }
  }

}
