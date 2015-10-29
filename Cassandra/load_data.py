from cassandra.cluster import Cluster

KS_NAME = 'big_data_analytics'
DATA_TABLE_NAME = 'data'
RF_TABLE_NAME = 'random_forest'
INDEX_NAME = 'data_type_index'


def prepare(session):
    # Check if data table exists
    ks_metadata = cluster.metadata.keyspaces[KS_NAME]
    if DATA_TABLE_NAME not in ks_metadata.tables:
        create_data_table(session)
    else:
        truncate_table(session, DATA_TABLE_NAME)

    # Check if index of data table exists
    table_metadata = ks_metadata.tables[DATA_TABLE_NAME]
    if INDEX_NAME not in table_metadata.indexes:
        create_data_table_index(session)

    # Check if random forest table exists
    if RF_TABLE_NAME not in ks_metadata.tables:
        create_rf_table(session)
    else:
        truncate_table(session, RF_TABLE_NAME)


def create_data_table(session):
    session.execute("""
    CREATE TABLE %s (
        row_num int primary key,
        features list<double>,
        label int,
        type ascii)
    """ % DATA_TABLE_NAME)


def create_rf_table(session):
    session.execute("""
    CREATE TABLE %s (
        rf_key ascii PRIMARY KEY,
        forest blob);
    """ % RF_TABLE_NAME)


def create_data_table_index(session):
    session.execute("""
    CREATE INDEX %s ON %s (type)
    """ % (INDEX_NAME, DATA_TABLE_NAME))


def truncate_table(session, table_name):
    session.execute("TRUNCATE TABLE %s" % table_name)


def load_data(session):
    with open('training', 'r') as f:
        row_num = 0
        for line in f.readlines():
            data = line.strip().split()
            features = [float(e) for e in data[:-1]]
            label = data[-1]
            session.execute(
                "INSERT INTO data (row_num, features, label, type)\
                       VALUES (%d, %s, %s, 'training')"
                % (row_num, features, label))
            row_num += 1

    with open('testing', 'r') as f:
        row_num = 0
        for line in f.readlines():
            data = line.strip().split()
            features = [float(e) for e in data[:-1]]
            label = data[-1]
            session.execute(
                "INSERT INTO data (row_num, features, label, type)\
                       VALUES (%d, %s, %s, 'testing')"
                % (row_num, features, label))
            row_num += 1


if __name__ == "__main__":
    cluster = Cluster()
    session = cluster.connect('big_data_analytics')
    prepare(session)
    load_data(session)
