package uk.ac.ebi.interpro.common.database;

import uk.ac.ebi.interpro.common.*;
import uk.ac.ebi.interpro.common.performance.*;
import uk.ac.ebi.interpro.common.collections.*;

import javax.sql.*;
import java.sql.*;
import java.util.*;
import java.io.*;
import java.net.*;
import java.lang.ref.WeakReference;

public class ConnectionManager implements /*DataSource,*/Runnable {

    public static List<ConnectionManager> archive=new ArrayList<ConnectionManager>();

    private static Location me=new Location();


    public int expiredCount;
    public int createdCount;
    final List<ConnectionRecord> spare=new ArrayList<ConnectionRecord>();
    final List<ConnectionRecord> active=new ArrayList<ConnectionRecord>();
    private DataSource source;
    String testSQL;
    String failMessage;
    private Interval timeout;
    public long lastChecked=0;

    public String name;

    public ConnectionManager(DataSource source, Map<String,String> config) {
        this(source,config.get("testSQL"),config.get("testFail"), new Interval(StringUtils.nvl(config.get("timeout"),"10m")));
        this.name=source.toString();
        archive.add(this);
    }

    public ConnectionManager(DataSource source, String testSQL, String failMessage,Interval timeout) {
        this.source = source;
        this.testSQL = testSQL;
        this.failMessage = failMessage;
        this.timeout = timeout;
    }



    public void check() {
        lastChecked=System.currentTimeMillis();

        long timeout=this.timeout.getMillis();
        expire(timeout);
        
    }

    private void expire(long timeout) {
        List<ConnectionRecord> deceased=new ArrayList<ConnectionRecord>();
        synchronized(this) {
            expireList(deceased,timeout, spare);
            expireList(deceased,timeout, active);
        }
        closeAll(deceased);
    }

    private void closeAll(List<ConnectionRecord> deceased) {
        for (ConnectionRecord record : deceased) {
            record.closeConnection();
            expiredCount++;
        }
    }

    public void run() {
        check();
    }

    public void close() {
        expire(-1);
        source=null;
    }

    public boolean isOpen() {
        return source!=null;
    }


    private void expireList(List<ConnectionRecord> deceased, long timeout, List<ConnectionRecord> input) {
        for (Iterator<ConnectionRecord> it = input.iterator(); it.hasNext();) {
            ConnectionRecord record = it.next();
            if (timeout<0 || record.age()>timeout) {
                it.remove();
                deceased.add(record);
            }
        }
    }



    public ConnectionRecord getConnectionRecord() throws SQLException {
        if (!isOpen()) throw new SQLException("Connection manager closed");
        Action a=me.start("Get Connection "+source.toString());
        try {
            while (true) {
                try {
                    synchronized(this) {
                        if (!spare.isEmpty()) {
                            Action a2=me.note("Spare");
                            return spare.remove(0).test();
                        }
                        else break;
                    }
                } catch (SQLException e) {
                    //ignore, it's an old connection
                }
            }
            Action a2=me.note("New");
            createdCount++;
            return new ConnectionRecord(this, source.getConnection()).test();
        } finally {
            me.stop(a);
        }

    }

    public Connection getConnection() throws SQLException {
        return getConnectionRecord().connection;
    }

    public void finish(Connection connection) {
        try {
            connection.rollback();
        } catch (Exception e) {
            try {
                connection.close();
            } catch (SQLException e1) {
                //nevermind!
            }
            return;
        }
        synchronized(this) {
            ConnectionRecord record = new ConnectionRecord(this, connection);
            spare.add(record);
            active.remove(record);
        }
    }

    public String toString() {
        return name+" open:"+isOpen()+" active:"+active.size()+" spare:"+spare.size()+" expired:"+expiredCount+" created:"+createdCount
                +" checked:"+(lastChecked==0?"not":Interval.getTextFromMillis(System.currentTimeMillis()-lastChecked))
                +" timeout:"+timeout;
    }

    public Connection getConnection(String s, String s1) throws SQLException {return source.getConnection(s, s1);}
    public PrintWriter getLogWriter() throws SQLException {return source.getLogWriter();}
    public void setLogWriter(PrintWriter printWriter) throws SQLException {source.setLogWriter(printWriter);}
    public void setLoginTimeout(int i) throws SQLException {source.setLoginTimeout(i);}
    public int getLoginTimeout() throws SQLException {return source.getLoginTimeout();}

    public static void main(String[] args) throws Exception {
        URL url=new File(args[0]).toURI().toURL();
        Map<String, String> props = CollectionUtils.loadPropertyMap(url);
        ConnectionManager cm=new ConnectionManager(new DataSourceCollection().get(url,props),props);
        Connection conn=cm.getConnection();
        cm.finish(conn);
        System.out.println(cm.toString());
    }
}
