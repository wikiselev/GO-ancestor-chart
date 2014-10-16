package uk.ac.ebi.interpro.common.database;

import javax.sql.DataSource;
import java.sql.*;
import java.util.Map;
import java.util.HashMap;
import java.util.Properties;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

public class DriverDataSource implements DataSource {

    public Driver driver;
    public String url;
    public Map<String,String> settings;
    private PrintWriter logWriter;
    private int loginTimeout;
    String[] exec=new String[0];

    public DriverDataSource(Driver d, String url) throws SQLException {
        this(d, url, null);
    }

    public DriverDataSource(Driver driver, String jdbcURL, Map<String,String> props) {
        this(driver, jdbcURL, props, null, null);

    }

    public DriverDataSource(Driver driver, String jdbcURL, String username, String password) {
        this(driver,jdbcURL,null,username, password);

    }


    public static Driver getDriver(Map<String,String> props) throws MalformedURLException, ClassNotFoundException, IllegalAccessException, InstantiationException {
        String driverURL = props.get("driverURL");
        String driverClassName = props.get("driverClass");
        ClassLoader cl;
        if (driverURL == null)
            cl = Thread.currentThread().getContextClassLoader();
        else
            cl = new URLClassLoader(new URL[]{new URL(driverURL)});

        return  (Driver) cl.loadClass(driverClassName).newInstance();

    }

    public DriverDataSource(String jdbcURL, Map<String,String> props, String username, String password) throws MalformedURLException, ClassNotFoundException, IllegalAccessException, InstantiationException, SQLException {
        this(getDriver(props),jdbcURL, props, username, password);
    }

    public DriverDataSource(Driver driver, String jdbcURL, Map<String,String> props, String username, String password) {
        if (props == null) props = new HashMap<String,String>();

        if (jdbcURL == null) jdbcURL = (String) props.get("jdbcURL");
        if (jdbcURL == null) jdbcURL = (String) props.get("url");

        if (username != null) props.put("user", username);
        if (password != null) props.put("password", password);

        this.url = jdbcURL;
        this.driver = driver;

        String init = props.get("init");
        if (init!=null)
            exec=init.split("\n");

        this.settings = props;
    }



    public Connection getConnection() throws SQLException {

        Properties p=new Properties();
        p.putAll(settings);
        return connect(p);
    }

    private Connection connect(Properties p) throws SQLException {
        Connection connection = driver.connect(url, p);
        connection.setAutoCommit(false);
        for (int i = 0; i < exec.length; i++) {
            PreparedStatement stmt = connection.prepareStatement(exec[i]);
            stmt.execute();
            stmt.close();
        }
        return connection;
    }

    public Connection getConnection(String username, String password) throws SQLException {
        Properties p=new Properties();
        p.putAll(settings);
        p.put("user", username);
        p.put("password", password);
        return connect(p);
    }

    public PrintWriter getLogWriter() throws SQLException {
        return logWriter;
    }

    public void setLogWriter(PrintWriter logWriter) throws SQLException {
        this.logWriter = logWriter;
    }

    public void setLoginTimeout(int loginTimeout) throws SQLException {
        this.loginTimeout = loginTimeout;
    }

    public int getLoginTimeout() throws SQLException {
        return loginTimeout;
    }

    public String toString() {
        return driver.getClass().getCanonicalName()+" "+settings.get("user")+"@"+url;
    }

    public <T> T unwrap(Class<T> tClass) throws SQLException {
        throw new SQLException("Is not a wrapper for "+tClass);
    }

    public boolean isWrapperFor(Class<?> aClass) throws SQLException {
        return false;
    }
}
