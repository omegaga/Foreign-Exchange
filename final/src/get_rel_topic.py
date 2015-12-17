import sys
import os
import json

PARAM_DIR = 'parameters/'
JAVA_DIR = 'search/'
RES_DIR = 'result/'
TMP_DIR = 'tmp/'

CONF_PATH = PARAM_DIR + 'parameterfile'
RESULT_PATH = TMP_DIR + 'tmp_result'
QUERY_PATH = TMP_DIR + 'tmp_query'
LAYER1_EXP_QRY = TMP_DIR + 'exp1_query'
LAYER2_EXP_QRY = TMP_DIR + 'exp2_query'
FB_DOCS = 50
FB_TERMS = 8


def get_bm25_conf(expQueryPath):
    conf = {
        "indexPath": "./index",
        "queryFilePath": QUERY_PATH,
        "trecEvalOutputPath": RESULT_PATH,
        "retrievalAlgorithm": "Indri",
        "BM25:k_1": 1.2,
        "BM25:b": 0.8,
        "BM25:k_3": 0.0,
        "Indri:lambda": 0.4,
        "Indri:mu": 2500,
        "fb": "true",
        "fbDocs": FB_DOCS,
        "fbTerms": FB_TERMS,
        "fbMu": 0,
        "fbOrigWeight": 0.0,
        "fbExpansionQueryFile": expQueryPath
        }
    with open(CONF_PATH, 'w') as fp:
        for k, v in conf.iteritems():
            fp.write("%s=%s\n" % (k, v))


def get_term_weight(line, base_weight, used_terms):
    total = 0
    term_dict = {}
    terms = line.split()[2:-1]

    for i in range(0, len(terms), 2):
        if terms[i + 1] in used_terms:
            continue
        term_dict[terms[i + 1]] = float(terms[i])
        total += float(terms[i])

    # Normalization
    for term in term_dict:
        term_dict[term] = term_dict[term] / total * base_weight

    return term_dict


def get_query_file(queryTerm):
    with open(QUERY_PATH, 'w') as fp:
        fp.write("1:" + queryTerm + ".keywords")


def gen2LevelTopics(queryTerm):

    used_terms = set()
    used_terms.add(queryTerm)

    print "1st level"
    get_query_file(queryTerm)
    get_bm25_conf(LAYER1_EXP_QRY)
    os.system("java -cp \"./search:./lucene-4.3.0/*\" QryEval " + CONF_PATH)

    with open(LAYER1_EXP_QRY, 'r') as fp:
        line = fp.readline()
        term_dict = get_term_weight(line, 10000.0, used_terms)

        for t in term_dict:
            used_terms.add(t)

    print "2nd level"
    term_dictL2 = {}

    for term in term_dict:
        print "term: " + term
        get_query_file(term)
        get_bm25_conf(LAYER2_EXP_QRY)
        os.system("java -cp \"./search:./lucene-4.3.0/*\" QryEval " +
                  CONF_PATH)
        with open(LAYER2_EXP_QRY, 'r') as fp:
            line = fp.readline()
            term_dictL2[term] =\
                get_term_weight(line, term_dict[term], used_terms)

    return term_dictL2


def get_json(term_dict, term):
    json_dict = {
        'name': term,
        'children': []
    }

    for term in term_dict:
        json_dict['children'].append({
            'name': term,
            'children': [{'name': term2, 'size': term_dict[term][term2]}
                         for term2 in term_dict[term]]
            })

    return json_dict

if __name__ == "__main__":
    if(len(sys.argv) != 3):
        print "Usage: python BdaFinal.py <query term> <output json file>"
        exit(-1)

    query = sys.argv[1]
    output_filename = sys.argv[2]
    term_dict = gen2LevelTopics(query)
    json_dict = get_json(term_dict, query)

    with open(output_filename, 'w') as fp:
        fp.write(json.dumps(json_dict))
