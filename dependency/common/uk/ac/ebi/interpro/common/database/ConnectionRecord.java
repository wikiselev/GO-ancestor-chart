package uk.ac.ebi.interpro.common.database;

import uk.ac.ebi.interpro.common.performance.Action;
import uk.ac.ebi.interpro.common.performance.ExceptionRecord;
import uk.ac.ebi.interpro.common.performance.Location;

import java.io.Closeable;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.PreparedStatement;

/**
 * Created by IntelliJ IDEA.
* User: dbinns
* Date: 22-Apr-2009
* Time: 14:06:36
* To change this template use File | Settings | File Templates.
*/
public class ConnectionRecord implements Closeable {

    private static Location me=new Location();


    public final Connection connection;
    private long changed;
    private ConnectionManager connectionManager;

    ConnectionRecord test() throws SQLException {
        Action t= me.start("Test");
        try {
            if (connectionManager.testSQL!=null) {
                me.note("TestSQL", connectionManager.testSQL);
                PreparedStatement ps = connection.prepareStatement(connectionManager.testSQL);
                ps.setQueryTimeout(10);
                if (!ps.executeQuery().next()) throw new SQLException(connectionManager.failMessage==null?"Test failed": connectionManager.failMessage);
                ps.close();
            }
            touch();
            synchronized(this) {
                connectionManager.active.add(this);}
            return this;
        } catch (SQLException e) {
            ExceptionRecord.note("Test failed",e);
            closeConnection();
            throw e;
        } finally {
            me.stop(t);
        }
    }

    public ConnectionRecord(ConnectionManager connectionManager, Connection connection) {
        this.connectionManager = connectionManager;
        this.connection = connection;
        touch();
    }

    synchronized void touch() {changed=System.currentTimeMillis();}

    synchronized long age() {
        return System.currentTimeMillis()-changed;
    }


    public boolean equals(Object o) {
        return connection== ((ConnectionRecord) o).connection;
    }

    public void closeConnection() {
        try {
            connection.close();
        } catch (SQLException e) {
            //nevermind
        }
    }

    public void close() {
        connectionManager.finish(connection);
    }
}
