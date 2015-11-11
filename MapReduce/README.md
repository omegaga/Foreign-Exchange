# MapReduce

Implements MapReduce that read data from Cassandra, then grow decision trees and aggregate into a random forest

## Organization:
This program consists of the following files:

* `RandomForestMapRed.java`: A class implementing Mapper, Reducer, and Driver
* `RandomForest.java`: A class implementing algorithm of random forest, including training data by growing decision trees, and testing data by calculate voting of the trees.
* `DataRow.java`: A class representing a row in data sets, which consists of a list of features and a label associated with the features.
* `TreeNode.java`: Implements a node class of decision tree, and operations on it.
* `FeatureLabelTuple.java`: A tuple class with store a selected feature and a label. Used for sorting when finding threshold of a feature.
