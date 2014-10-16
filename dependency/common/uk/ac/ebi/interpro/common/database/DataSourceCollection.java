package uk.ac.ebi.interpro.common.database;

import uk.ac.ebi.interpro.common.*;
import uk.ac.ebi.interpro.common.collections.*;
import uk.ac.ebi.interpro.common.database.pool.*;

import java.sql.*;
import java.net.*;
import java.io.*;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: dbinns
 * Date: Mar 1, 2006
 * Time: 11:53:03 PM
 * To change this template use File | Settings | File Templates.
 */
public class DataSourceCollection {

    public Map drivers = new HashMap();

    public Driver addURLDriver(String name, URL url, String className) throws ClassNotFoundException, IllegalAccessException, InstantiationException, SQLException {
        Driver driver = (Driver) new URLClassLoader(new URL[]{url}).loadClass(className).newInstance();
        drivers.put(name, driver);
        DriverManager.deregisterDriver(driver);
        return driver;
    }

/*

    public PoolDataSource getPool(URL definition,Map<String,String> props) throws Exception {
        return new PoolDataSource(get(definition,props),props);
    }
    public PoolDataSource getPool(URL definition) throws Exception {
        Map<String,String> props= CollectionUtils.loadPropertyMap(definition);
        return new PoolDataSource(get(definition,props),props);
    }
*/
    public DriverDataSource get(File definition) throws Exception {
        return get(definition.toURI().toURL());
    }
    public DriverDataSource get(URL definition) throws Exception {
        return get(definition,null);
    }


    public DriverDataSource get(URL definition,Map<String,String> props) throws Exception {

        if (props==null) props=CollectionUtils.loadPropertyMap(definition);


        String url=props.get("url");

        String password=props.get("password");
        String passwordFile=props.get("passwordFile");
        if (password==null && passwordFile!=null) {
            URL pwurl = getURL(definition, passwordFile);
            password = IOUtils.readString(pwurl).trim();
            if (password==null) {
                throw new SQLException("Password could not be loaded from "+pwurl);
            }
        }
        if (password!=null) props.put("password", password);


        String driverJar=props.get("driverJar");
        String driverClass=props.get("driverClass");
        Driver driver;
        if (driverJar!=null) {
            driver = (Driver) drivers.get(driverJar);
            if (driver==null) {
                driver=addURLDriver(driverJar,getURL(definition,driverJar),driverClass);
            }
        } else {
            driver=(Driver) getClass().getClassLoader().loadClass(driverClass).newInstance();
        }

        return new DriverDataSource(driver,url,props);

    }

    private URL getURL(URL definition, String text) throws MalformedURLException {
        if (definition == null) return new URL(text);
        else return new URL(definition, text);
    }

    public static String syntaxDriverDataSource=""+
            "driverClass \n" +
            "  Java class name of driver (required) \n" +
            "  driverJar\tLocation of Jar file containing driver (optional - alternative is to include driver JAR into runtime classpath).\n" +
            "user \n" +
            "  Database user name (required)\n" +
            "password \n" +
            "  Property containing password (not recommended, acceptible for known passwords)\n" +
            "passwordFile\n" +
            "  Location of file containing password (make it non-accessible to other users)\n" +
            "url \n" +
            "  JDBC URL. Often\n" +
            "  jdbc:oracle:thin:@host:port:SID\n" +
            "init\n" +
            "  SQL statement (not query) to be executed on initialisation.\n" +
            "  Recommended for oracle:\n" +
            "  begin dbms_application_info.set_module('application','module'); end;";

    /*public static String syntaxPoolDataSource=syntaxDriverDataSource+
            "maxTimeActive\n" +
            "  time for which a connection may be active.\n" +
            "maxActive\n" +
            "  maximum number of active connections\n" +
            "maxTimeIdle\n" +
            "  maximum time that a connection may be idle before being closed\n" +
            "maxTimeWait \n" +
            "  maximum waiting time to obtain a connection while database has maximum number of active connections\n" +
            "checkFrequency\n" +
            "  time between checks of existing connections\n" +
            "Should be less than maxTimeActive and maxTimeIdle\n" +
            "maxWaitConnect\n" +
            "  maximum time to wait while creating new connection to database\n" +
            "testSQL\n" +
            "  SQL query to be executed to test a database connection prior to returning it to the caller\n" +
            "  Should return at least one row if database is OK\n";
*/


}
