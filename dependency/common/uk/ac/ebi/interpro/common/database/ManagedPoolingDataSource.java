package uk.ac.ebi.interpro.common.database;

//import org.apache.commons.dbcp.*;
//import org.apache.commons.pool.impl.*;
//import org.apache.commons.pool.*;
//
//import javax.sql.*;
//import java.sql.*;
//import java.io.*;
//import java.util.*;
//import java.net.*;
//
//import uk.ac.ebi.interpro.common.*;

public class ManagedPoolingDataSource/* extends PoolingDataSource*/ {
//
//    /* see:
//    http://jakarta.apache.org/commons/pool/apidocs/index.html
//    */
//    public GenericObjectPool connectionPool;
//    PoolableConnectionFactory poolableConnectionFactory;
//    private KeyedObjectPool stmtpool;
//    private String module;
//
//
//    public ManagedPoolingDataSource(final ConnectionFactory underlying,String testStatement) {
//        super();
//
//
//        connectionPool=new GenericObjectPool(null){
//            public void returnObject(Object o) throws Exception {
//                if (isClosed())
//                    poolableConnectionFactory.destroyObject(o);
//                else
//                    super.returnObject(o);
//            }
//        };
//
//        setPool(connectionPool);
//
//
//        connectionPool.setMinEvictableIdleTimeMillis(10000);// Idle 10s
//        connectionPool.setTimeBetweenEvictionRunsMillis(10000);//test every 10s
//        connectionPool.setNumTestsPerEvictionRun(-1);//test all
//
//
//        connectionPool.setTestOnBorrow(true);
//
//        final GenericKeyedObjectPoolFactory statementPoolFactory = new GenericKeyedObjectPoolFactory(
//                null,
//                -1, // unlimited maxActive (per key)
//                GenericKeyedObjectPool.WHEN_EXHAUSTED_FAIL,
//                0, // maxWait
//                1, // maxIdle (per key)
//                -1 //no maximum
//        );
//
//
//
//
//
//        ConnectionFactory statementPoolingConnectionFactory = new ConnectionFactory(){
//            public Connection createConnection() throws SQLException {
//                stmtpool = statementPoolFactory.createPool();
//
//                PoolingConnection poolingConnection = new PoolingConnection(underlying.createConnection(),stmtpool) {
//                    public synchronized void close() throws SQLException {
//                        try {
//                            stmtpool.close();
//                        } catch (Exception e) {
//                            //todo: ?
//                        }
//                        getDelegate().close();
//                    }
//                };
//                stmtpool.setFactory(poolingConnection);
//                return poolingConnection;
//            }
//        };
//
//        //statementPoolingConnectionFactory=underlying;
//
//        poolableConnectionFactory = new PoolableConnectionFactory(
//                statementPoolingConnectionFactory, // from which new connections will be obtained
//                connectionPool, // pool to cache the connections
//                null, // no prepared statement cache
//                testStatement, // executed before connection use
//                false, // readonly
//                false // autocommit
//        );
//
//
//
////        poolableConnectionFactory = new PoolableConnectionFactory(
////                        underlying, // from which new connections will be obtained
////                        connectionPool, // pool to cache the connections
////                        statementPoolFactory, // no prepared statement cache
////                        testStatement, // no test statement
////                        false, // readonly
////                        false // autocommit
////                );
//
//
//    }
//
//    public ManagedPoolingDataSource(ConnectionFactory connectionFactory, String testStatement, String module) {
//        this(connectionFactory, testStatement);
//        this.module = module;
//    }
//
//    public void close() {
//        try {
//            connectionPool.close();
//        } catch (Exception e) {
//            //todo: probably log this somewhere
//        }
//    }
//
//    public Connection getConnection(String action) throws SQLException {
//        Connection conn=super.getConnection();
//        PreparedStatement pstmt=conn.prepareStatement("begin dbms_application_info.set_module(?,?);end;");
//        pstmt.setString(1,module);
//        pstmt.setString(2,action);
//        pstmt.executeUpdate();
//        pstmt.close();
//        return conn;
//    }
//
//
//    public static ConnectionFactory get(Map config) throws Exception {
//        return new DataSourceConnectionFactory(new DriverDataSource(null,CollectionUtils.mapToProperties(config),null,null));
//
//    }
//
//    public static ConnectionFactory get(Map config,Driver driver) throws SQLException {
//
//        return new DataSourceConnectionFactory(new DriverDataSource(driver,null,CollectionUtils.mapToProperties(config)));
//
//    }
//
//    public static void main(String[] args) throws Exception, IOException {
//        //System.out.println(Long.parseLong(Long.toString(-1, 16), 16));
//
//
//
//
//        //Map config=StringUtils.extract("driverClass=oracle.jdbc.driver.OracleDriver;jdbcURL=jdbc:oracle:thin:@cumae.ebi.ac.uk:1531:IPRO;user=ops$dbinns;password=opsdbinns;smtp=mailserv.ebi.ac.uk;admin=dbinns@ebi.ac.uk;test=select * from dual;client=me");
//        Map config=StringUtils.extract("driverClass=oracle.jdbc.driver.OracleDriver;jdbcURL=jdbc:oracle:thin:@manea.ebi.ac.uk:1531:STORM;user=ops$dbinns;password=opsdbinns;smtp=mailserv.ebi.ac.uk;admin=dbinns@ebi.ac.uk;test=select * from dual;client=me");
//
//        ManagedPoolingDataSource ds=new ManagedPoolingDataSource(ManagedPoolingDataSource.get(config),(String) config.get("test"),(String) config.get("client"));
//
//        System.out.println("Connect");
//
//        Connection conn=ds.getConnection("doing stuff");
//
//        PreparedStatement ps = conn.prepareStatement("select * from dual");
//        ResultSet rset=ps.executeQuery();
//        System.out.println("Has rows?"+rset.next());
//
//        System.in.read();
//
//        rset.close();
//
//        ds.connectionPool.setMaxIdle(1);
//
//        //ps.close();
//        conn.close();
//        conn=ds.getConnection();
//
//        rset = conn.prepareStatement("select * from dual").executeQuery();
//        System.out.println("Has rows?"+rset.next());
//        rset.close();
//
//        ds.connectionPool.setMaxIdle(0);
//
//
//        try {
//            conn.close();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//        System.out.println("Closing");
//
//
//        try {
//            ds._pool.close();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//
//        System.out.println("Done");
//
//
//    }
//
//
//
//
//
}
