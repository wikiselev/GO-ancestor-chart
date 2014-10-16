package uk.ac.ebi.interpro.common.database;

import uk.ac.ebi.interpro.common.*;

import java.lang.reflect.*;
import java.sql.*;
import java.util.*;

public class SQLUtils {

    /**
     * Set string parameters of a prepared statement
     *
     * @param ps             Target statement
     * @param parameterNames Zero based indexed list of names of parameters to replace
     * @param data           Map containing values to set
     * @throws SQLException on underlying error
     */

    public static void bind(PreparedStatement ps, List<String> parameterNames, Map<String, BindVariable> data) throws SQLException {
        for (int i = 0; i < parameterNames.size(); i++)
            if (data.containsKey(parameterNames.get(i))) {
                BindVariable bv = data.get(parameterNames.get(i));
                ps.setObject(i + 1, bv.value, bv.type);
            }
    }

    /**
     * Retrieve all data as a list of maps.
     *
     * @param rset Source of data
     * @return List of data
     * @throws SQLException on underlying error
     */

    public static List<Map<String, String>> queryListMap(ResultSet rset) throws SQLException {
        List<Map<String, String>> l = new ArrayList<Map<String, String>>();
        while (rset.next())
            l.add(rowMap(rset));
        return l;
    }

    /**
     * Extract list of first column values from ResultSet
     *
     * @param rset from which all rows will be extracted
     * @return list of first column values
     * @throws SQLException on underlying error
     */

    public static List<String> queryListString(ResultSet rset) throws SQLException {

        List<String> list = new ArrayList<String>();
        while (rset.next()) list.add(rset.getString(1));
        return list;
    }


    /**
     * Execute command.
     *
     * @param conn    Connection with which to execute
     * @param command SQL to execute
     * @throws SQLException on underlying error
     */

    public static void execute(Connection conn, String command) throws SQLException {
        PreparedStatement pstmt = conn.prepareStatement(command);
        pstmt.executeUpdate();
        pstmt.close();
    }

    /**
     * Return first column of first row as String.
     * ResultSet will be closed.
     *
     * @param rset ResultSet as source of data
     * @return first column of first row
     * @throws SQLException on underlying error
     */
    public static String queryString(ResultSet rset) throws SQLException {
        if (!rset.next()) return null;
        String s = rset.getString(1);
        rset.close();
        return s;
    }

    /**
     * Retrieve current row of a resultset into a map
     *
     * @param rset Resultset from which data will be loaded
     * @return Map of column name-field value pairs
     * @throws SQLException on underlying error
     */

    public static Map<String, String> rowMap(ResultSet rset) throws SQLException {
        Map<String, String> init = new HashMap<String, String>();
        ResultSetMetaData rmd = rset.getMetaData();
        int ct = rmd.getColumnCount();
        for (int i = 1; i <= ct; i++) {
            init.put(rmd.getColumnLabel(i), StringUtils.nvl(rset.getString(i)));
        }
        return init;
    }

    /**
     * Load query results into objects using introspection.
     * <p/>
     * Executes the query and for each row creates an instance of the object.
     * A suitable constructor is chosen given the types of the supplied parameters.
     * Inner classes must have a reference to an instance of their containing class.
     * The data for each column is put in a field in the object having name as the column label.
     *
     * @param rset   source
     * @param c      Class of objects to create
     * @param params Extra parameters to pass to constructor
     * @return A list of objects of the specified class
     * @throws java.sql.SQLException  if query fails
     * @throws IllegalAccessException if unable to set field or call constructor
     * @throws InstantiationException if unable to create instance of class
     * @throws java.lang.reflect.InvocationTargetException
     *                                if constructor throws exception
     * @throws NoSuchMethodException  if no suitable constructor is found
     */

    public static <X> List<X> queryListClass(ResultSet rset, Class<X> c, Object... params) throws SQLException, IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {


        Field[] fields = getIntrospectionMapping(rset, c);
        List<X> list = new ArrayList<X>();
        Class<?>[] parameterTypes = new Class[params.length];
        for (int i = 0; i < params.length; i++) parameterTypes[i] = params[i].getClass();
        Constructor<X> cst = c.getConstructor(parameterTypes);
        while (rset.next()) {
            X o = cst.newInstance(params);
            set(fields, o, rset);
            list.add(o);
        }
        return list;

    }

    private static Field[] getIntrospectionMapping(ResultSet rset, Class<?> c) throws SQLException {
        ResultSetMetaData rmd = rset.getMetaData();
        int ct = rmd.getColumnCount();
        Field[] fields = new Field[ct];
        for (int i = 1; i <= ct; i++) {
            try {
                fields[i - 1] = c.getField(rmd.getColumnLabel(i));
            } catch (NoSuchFieldException e) {
                fields[i - 1] = null;
            }
        }
        return fields;
    }


    public static void set(Field[] fields, Object o, ResultSet rset) throws IllegalAccessException, SQLException {
        for (int i = 1; i <= fields.length; i++) {
            if (fields[i - 1] == null) continue;
            Class<?> fieldClass = fields[i - 1].getType();
            if (fieldClass.isAssignableFrom(String.class)) fields[i - 1].set(o, StringUtils.nvl(rset.getString(i)));
            else if (fieldClass.isAssignableFrom(Integer.TYPE)) fields[i - 1].set(o, rset.getInt(i));
            else fields[i - 1].set(o, rset.getObject(i));
        }
    }


    public static <X> X set(X o, ResultSet rset) throws SQLException, IllegalAccessException {
        set(getIntrospectionMapping(rset, o.getClass()), o, rset);
        return o;
    }


    public static <X> X queryClass(ResultSet rset, Class<X> c, Object... params) throws SQLException, IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
        List<X> l = queryListClass(rset, c, params);
        if (l.isEmpty()) return null;
        return l.get(0);
    }


    /**
     * Query and return map of column name-field string value pairs for current row.
     *
     * @param rset ResultSet of which the current row will be read
     * @return map of data
     * @throws SQLException if rset.getString(columnName) fails
     */


    public static Map<String, String> queryMap(ResultSet rset) throws SQLException {
        if (!rset.next()) return null;
        Map<String, String> m = rowMap(rset);
        rset.close();
        return m;
    }


}
