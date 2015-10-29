# Integration with Cassandra

In this project, we integrate our random forest model with cassandra.

## Updated code structure

* load_data.py: Load data from output file of training set and test set to cassandra.
* Main.java: Read data from cassendra (produced by load_data.py, and store serialized random forest in cassendra.
