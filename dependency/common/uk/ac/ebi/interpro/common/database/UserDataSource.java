package uk.ac.ebi.interpro.common.database;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.io.PrintWriter;

public class UserDataSource implements DataSource {
    private DataSource source;
    private String username;
    private String pass;

    public UserDataSource(DataSource source, String username, String pass) {

        this.source = source;
        this.username = username;
        this.pass = pass;
    }

    public Connection getConnection() throws SQLException {
        if (username==null || pass==null) throw new SQLException("Username/password not set");
        return source.getConnection(username,pass);
    }

    public Connection getConnection(String username, String pass) throws SQLException {
        return source.getConnection(username, pass);
    }

    public PrintWriter getLogWriter() throws SQLException {
        return source.getLogWriter();
    }

    public void setLogWriter(PrintWriter printWriter) throws SQLException {
        source.setLogWriter(printWriter);
    }

    public void setLoginTimeout(int i) throws SQLException {
        source.setLoginTimeout(i);
    }

    public int getLoginTimeout() throws SQLException {
        return source.getLoginTimeout();
    }


    public <T> T unwrap(Class<T> tClass) throws SQLException {
        throw new SQLException("Is not a wrapper for "+tClass);
    }

    public boolean isWrapperFor(Class<?> aClass) throws SQLException {
        return false;
    }

}
