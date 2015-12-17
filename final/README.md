Files in /:
src/: directory containing source files
demo/: a demo of data visualization
notebook.pdf: notebook for this project


Files in src/:
indexBuilder/: a Java project to build index using Apache Lucene
lucene-4.3.0/: library for lucene
parameters/: parameters for our search engine
search/: A search engine based on Apache Lucene to generate expanded query
tmp/: temporary directory for query expansion
visualize/: HTML files to visualize results
get_rel_topic.py: a Python script to read user's query and generate result JSON file by calling search module


Files in indexBuilder/:
lib/: dependencies of index builder
src/Main.java: Execute the index builder
src/IndexBuilder.java: Iterate through files in directory and build indexes from documents
src/DocumentExtractor.java: Read necessary information from document XML


Selected files in search/:
QryEval.java: The Main class of this project, execute the query
Qry.java: Root node of the query AST node
QryExpansion.java: Expand the query based on probabilistic language model


Usage:
1. Build index: go to indexBuilder, compile the project, and execute `java -cp ":./lib/*" Main <document_path> <index_path>`
2. Link the index: `ln -s <path_to_index> src/index`
3. Compile search module using Makefile
4. Execute query: `python get_rel_topic.py <query> visualize/data.json`
5. Open visualize/index.html in Safari or Firefox
