# Decision Tree

Implements the decision tree with ID3 algorithm.

## Organization:
This program consists of the following files:

* `DecisionTree.java`: Main class of this program. Read data, train the model with training set and validate with testing set.
* `DataRow.java`: A class representing a row in data sets, which consists of a list of features and a label associated with the features.
* `TreeNode.java`: Implements a node class of decision tree, and operations on it.
* `FeatureLabelTuple.java`: A tuple class with store a selected feature and a label. Used for sorting when finding threshold of a feature.
