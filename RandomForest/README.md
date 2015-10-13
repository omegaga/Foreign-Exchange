# Random Forest

Implements random forest based on decision tree

## Organization:
This program consists of the following files:

* `RandomForest.java`: A class implementing algorithm of random forest, including training data by growing decision trees, and testing data by calculate voting of the trees.
* `Main.java`: Main class of this program. Read data, train the model with training set, validate with testing set and serialize the random forest for later use.
* `DataRow.java`: A class representing a row in data sets, which consists of a list of features and a label associated with the features.
* `TreeNode.java`: Implements a node class of decision tree, and operations on it.
* `FeatureLabelTuple.java`: A tuple class with store a selected feature and a label. Used for sorting when finding threshold of a feature.
