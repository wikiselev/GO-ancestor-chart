package uk.ac.ebi.interpro.common.database;

import java.sql.*;

public class SQLConnection {
    Connection connection;
    QuerySet sql;


    public SQLConnection(Connection connection, QuerySet sql) {
        this.connection = connection;
        this.sql = sql;
    }

    SQLCommand open(String name) {
        return sql.get(name).setConnection(connection);         
    }
}
