/**
 *  Copyright (c) 2015, Carnegie Mellon University.  All Rights Reserved.
 */

import java.io.*;
import java.util.ArrayList;

/**
 *  The AND operator for all retrieval models.
 */
public abstract class QrySopWithWeight extends QrySop {
  protected ArrayList<Double> weights = new ArrayList<>();
  protected Double weightSum = 0.0;

  public void appendWeight(Double weight) {
    weights.add(weight);
    weightSum += weight;
  }

  @Override public String toString() {

    String result = "";

    for (int i=0; i<this.args.size(); i++) {
      result += String.format("%.4f", this.weights.get(i)) + " ";
      result += this.args.get(i) + " ";
    }

    return (this.getDisplayName() + "( " + result + ")");
  }
}
