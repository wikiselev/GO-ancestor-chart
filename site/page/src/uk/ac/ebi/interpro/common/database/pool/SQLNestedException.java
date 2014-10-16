package uk.ac.ebi.interpro.common.database.pool;

import java.sql.*;

public class SQLNestedException extends SQLException {
    public SQLNestedException(String message, Throwable cause) {
        super(message);
        this.initCause(cause);
    }
}
