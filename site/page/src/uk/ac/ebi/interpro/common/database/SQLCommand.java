package uk.ac.ebi.interpro.common.database;

import uk.ac.ebi.interpro.common.*;
import uk.ac.ebi.interpro.common.performance.*;

import java.beans.*;
import java.io.*;
import java.lang.reflect.*;
import java.sql.*;
import java.util.*;

/**
 * Class encapsulating an SQL command with replaceable text and named bind variables.
 * <p/>
 * For example:
 * <p/>
 * select name from ${schema}.entry where entry_ac=?{entry_ac}
 * <p/>
 * Has one replacement ${schema} and one bind variable ?{entry_ac}
 */
public class SQLCommand implements Iterable<ResultSet> {

    private static Location me = new Location();

    private String name;
    public String formLocation;


    /**
     * text -> replaced (textual replacements) -> sql (bind variable names replaced with ?)
     */
    private String text;
    private String replaced;
    private String sql;

    private List<String> bindVariableNames = new ArrayList<String>();

    private Connection connection;
    private PreparedStatement pstmt;
    private ResultSet rset;
    private Map<String, String> replacements = new HashMap<String, String>();
    private Map<String, BindVariable> values = new HashMap<String, BindVariable>();
    public List<Batch> batch = new ArrayList<Batch>();
    public String[] valueNames = {};
    private List<Exception> failures;

    public Action execution;

    public static class Batch {
        public Object[] values;

        public Batch(String[] names, Map<String, BindVariable> v) {
            this.values = new BindVariable[names.length];
            for (int i = 0; i < names.length; i++) values[i] = v.get(names[i]);
        }
    }

    int batchCount = 0;

    public static Seeker make(String definition) {
        if (definition == null) return null;
        definition = definition.trim();
        if (definition.startsWith("/") && definition.endsWith("/")) {
            return new RegexSeeker(definition.substring(1, definition.length() - 1));
        } else return null;
    }


    int batchSize = 0;

    public static final Seeker dollarCurly = new StringSeeker("${", "}");
    public static final Seeker questionCurly = new StringSeeker("?{", "}");
    public static final Seeker ampWord = new RegexSeeker("\\&([a-zA-Z_0-9]*)");
    public static final Seeker colonWord = new RegexSeeker("\\:([a-zA-Z_0-9]*)");
    public static final Seeker singleQuote = new RegexSeeker("'[^']*'");

    public Seeker replace = dollarCurly;
    public Seeker bind = questionCurly;
    public Seeker protect = null;

    public boolean emptyFailure=false;


    public SQLCommand setEmptyFailure(boolean emptyFailure) {
        this.emptyFailure = emptyFailure;
        return this;
    }

    public void setName(String name) {
        this.name = name;
    }


    /**
     * Prepare the statement.
     * All textual replacements must be supplied before this point.
     *
     * @throws SQLException on underlying error
     */

    public void prepare() throws SQLException {

        // check already prepared?
        if (replaced!=null) return;

        if (text == null || text.length() == 0) throw new SQLException("Empty query");

        bindVariableNames.clear();

        replaced = StringUtils.replaceParameters(replacements, null, text, replace, null, null, true);

        sql = StringUtils.replaceParameters(null, "?", replaced, bind, protect, bindVariableNames, false);

        execution=me.start(name,new SQLCommandInfo(this));

        pstmt = connection.prepareStatement(sql);

        valueNames = new TreeSet<String>(bindVariableNames).toArray(new String[bindVariableNames.size()]);
    }

    /**
     * Prepare the statement.
     * All textual replacements must be supplied before this point.
     *
     * @throws SQLException on underlying error
     * @return this command
     */

    public SQLCommand prepare(Connection conn) throws SQLException {
        setConnection(conn);
        prepare();
        return this;
    }


    /**
     * Bind the statement.
     * All bind variables must be supplied before this point.
     *
     * @throws SQLException on underlying error
     */

    public void bind() throws SQLException {
        SQLUtils.bind(pstmt, bindVariableNames, values);
        batch.add(new Batch(valueNames, values));
    }

    /**
     * Set the replace and bind variable formats.
     * @return this SQLCommand
     * @param replace Named textual replacement format
     * @param bind Named bind variable format
     * @param protect Don't replace or bind variables protected like this
     */

    public SQLCommand setFormat(Seeker replace, Seeker bind, Seeker protect) {
        this.protect = protect;
        this.bind = bind;
        this.replace = replace;
        return this;
    }

    /**
     * Process this command with SQLPlus syntax
     *
     * @return this
     */

    public SQLCommand setSQLPlusFormat() {
        return setFormat(ampWord, colonWord, singleQuote);
    }


    public SQLCommand setBatchSize(int batchSize) {
        this.batchSize = batchSize;
        return this;
    }


    /**
     * Register a list in which failures will be stored.
     *
     * @param failures User supplied list in which exceptions will be stored
     */
    public void setExceptionList(List<Exception> failures) {

        this.failures = failures;
    }

    /**
     * Set values for bind variables.
     *
     * @param values Map name-value
     * @return this SQLCommand
     */


    public SQLCommand setBindVariables(Map<String, String> values) {
        for (String name : values.keySet()) {
            bind(name,values.get(name));
        }
        return this;
    }


    /**
     * Set values for textual replacements.
     *
     * @param replacements Map name-value
     * @return this SQLCommand
     */

    public SQLCommand setReplacements(Map<String, String> replacements) {
        this.replacements.putAll(replacements);
        return this;
    }

    /**
     * Set individual textual replacement.
     *
     * @param name  Name of textual replacement.
     * @param value Value to substitute
     * @return this SQLCommand
     */

    public SQLCommand replace(String name, String value) {
        replacements.put(name, value);
        return this;
    }

    /**
     * Set individual bind variable
     *
     * @param name  Name of bind variable
     * @param value Value to substitute
     * @return this SQLCommand
     */

    public SQLCommand bind(String name, String value) {
        values.put(name, new BindVariable(value));
        return this;
    }

    public SQLCommand bind(String name, int value) {
        values.put(name, new BindVariable(value));
        return this;
    }

    public SQLCommand bind(String name, long value) {
        values.put(name, new BindVariable(value));
        return this;
    }


    public Iterator<ResultSet> iterator() {
        final Stack<SQLException> failure = new Stack<SQLException>();
        try {
            query();
        } catch (SQLException e) {
            failure.push(e);
            ExceptionRecord.note("Error in querying",e);
        }

        return new Iterator<ResultSet>() {

            public boolean hasNext() {
                if (rset==null) return !emptyFailure;
                try {
                    if (rset.next()) return true;
                    close();
                    rset=null;
                    return false;
                } catch (SQLException e) {
                    failure.push(e);
                    ExceptionRecord.note("Error in resultset",e);
                }
                return !emptyFailure;
            }

            public ResultSet next() throws NoSuchElementException {
                if (!failure.isEmpty()) throw wrap(failure.pop());
                if (rset==null) throw new NoSuchElementException("ResultSet exhausted");
                return rset;
            }

            private NoSuchElementException wrap(SQLException e) {
                NoSuchElementException e2 = new NoSuchElementException("Query failed");
                e2.initCause(e);
                return e2;
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }




    /**
     * Execute query.
     *
     * @return ResultSet
     * @throws SQLException on underlying error
     */

    public ResultSet query() throws SQLException {
        try {
            prepare();
            bind();
            return rset=pstmt.executeQuery();
        } catch (SQLException e) {
            close();
            if (failures != null) failures.add(e);
            throw new SQLCommandException(e);
        }
    }

    class SQLCommandException extends SQLException {


        public SQLCommandException(SQLException e) {
            super(e.getMessage());
            initCause(e);
        }


        public void printStackTrace() {
            printStackTrace(System.out);
        }
/*

        public void printStackTrace(PrintStream printStream) {
            PrintWriter printWriter = new PrintWriter(printStream);
            printStackTrace(printWriter);
            printWriter.flush();
        }

        public void printStackTrace(PrintWriter printWriter) {
            printAllStackTrace(printWriter);
        }

        public void printAllStackTrace(PrintWriter printWriter) {
            super.printStackTrace(printWriter);
            Exception e = getNextException();
            if (e != null) e.printStackTrace(printWriter);
        }

*/

        public String toString() {
            return name + " " + connection+" "+sql;
        }


    }





    /**
     * Create SQLCommand from SQL text
     *
     * @param name Name
     * @param text SQL
     * @param connection database connection
     */

    public SQLCommand(String name, String text,Connection connection) {
        this.connection = connection;
        this.name = name;
        this.text = text;
    }


    /**
     * Create SQLCommand from SQL text
     *
     * @param name Name
     * @param text SQL
     */

    public SQLCommand(String name, String text) {
        this.name = name;
        this.text = text;
    }

    /**
     * Create SQLCommand from SQL text
     *
     * @param text SQL
     */

    public SQLCommand(String text) {
        this("?",text);
    }


    /**
     * Set the connection to be used.
     *
     * @param connection to be used
     * @return this command
     */
    public SQLCommand setConnection(Connection connection) {
        this.connection = connection;
        return this;
    }

    /**
     * Update database and close prepared statement
     *
     * @throws SQLException on underlying failure
     */

    public void updateClose() throws SQLException {
        prepare();
        bind();
        pstmt.executeUpdate();
        close();
    }

    /**
     * Execute query and return first column of first row as String
     *
     * @return String
     * @throws SQLException if the query fails
     */

    public String queryString() throws SQLException {
        try {
            return SQLUtils.queryString(query());
        } finally {
            close();
        }
    }


    /**
     * Query and return map of column name-field value pairs for first row.
     *
     * @return map
     * @throws SQLException if the query fails
     */

    public Map<String, String> queryMap() throws SQLException {
        try {
            return SQLUtils.queryMap(query());
        } finally {
            close();
        }
    }

    /**
     * Query and return list of first column in all rows
     *
     * @return list of strings
     * @throws SQLException if the query fails
     */

    public List<String> queryListString() throws SQLException {
        try {
            return SQLUtils.queryListString(query());
        } finally {
            close();
        }
    }

    /**
     * Load all data as a list of maps.
     *
     * @return List of maps
     * @throws SQLException if the query fails
     */
    public List<Map<String, String>> queryListMap() throws SQLException {
        try {
            return SQLUtils.queryListMap(query());
        } finally {
            close();
        }
    }


    /**
     * Load query results into objects using introspection.
     * <p/>
     * Executes the query and for each row creates an instance of the object.
     * The data for each column is put in a field in the object having name as the lower case of the column name.
     *
     * @param c Class of objects to create
     * @param params constructor parameters
     * @return A list of objects of the specified class
     * @throws SQLException if the query fails
     * @throws IntrospectionException if the object cannot be created
     * @throws IllegalAccessException if the object cannot be created
     * @throws InstantiationException if the object cannot be created
     * @throws NoSuchFieldException if the object cannot be created
     * @throws NoSuchMethodException if the object cannot be created
     * @throws InvocationTargetException if the object throws and error while be created.
     */
    public <X> List<X> queryListClass(Class<X> c, Object... params) throws SQLException, IntrospectionException, IllegalAccessException, InstantiationException, NoSuchFieldException, NoSuchMethodException, InvocationTargetException {

        try {
            return SQLUtils.queryListClass(query(), c, params);
        } finally {
            close();
        }
    }


    public <X> X queryClass(Class<X> c, Object... params) throws SQLException, IntrospectionException, IllegalAccessException, InstantiationException, NoSuchFieldException, NoSuchMethodException, InvocationTargetException {

        try {
            return SQLUtils.queryClass(query(), c, params);
        } finally {
            close();
        }
    }

    public PreparedStatement getStatement() {
        return pstmt;
    }

    /**
     * Add batch
     *
     * @throws SQLException on underlying error
     */

    public void addBatch() throws SQLException {
        bind();
        pstmt.addBatch();
        batchCount++;
        if (batchCount == batchSize) {
            executeBatch();
        }
    }

    /**
     * Execute batch
     *
     * @throws SQLException if execution fails
     */

    public void executeBatch() throws SQLException {
        if (batchCount == 0) return;
        batchCount = 0;
        Action execution = me.start("batchsize "+batchCount);
        pstmt.executeBatch();
        me.stop(execution);

        batch.clear();
    }



    public void close() throws SQLException {
        if (batchCount > 0) executeBatch();
        if (pstmt!=null) pstmt.close();
        me.stop(execution);
    }


    public SQLCommand setMaxRows(int maxRows) throws SQLException {
        pstmt.setMaxRows(maxRows);
        return this;
    }


    public SQLCommand setQueryTimeout(int timeout) throws SQLException {
        pstmt.setQueryTimeout(timeout);
        return this;
    }

    public String getOriginalText() {
        return text;
    }

    public String toString() {
        return name;
    }

    public static class SQLCommandInfo implements ToHTML {

        String formLocation;
        String replaced;
        String[] names;
        BindVariable[][] values;

        public SQLCommandInfo(SQLCommand sql) {
            formLocation = sql.formLocation;
            replaced = sql.replaced;
            names = sql.valueNames;
            values = new BindVariable[sql.batch.size()][names.length];
            for (int i = 0; i < values.length; i++) {
                for (int j = 0; j < values[i].length; j++) {
                    values[i][j] = sql.values.get(names[j]);
                }
            }

        }

        public void toHTML(PrintWriter wr) {
            /*wr.print("<form action='");
            wr.print(formLocation);
            wr.print("' method='POST' target='_blank'>");

            wr.print("<table><tr>");
            for (String name : names) wr.print("<td>" + name + "</td>");
            wr.print("</tr>");
            for (BindVariable[] value : values) {
                wr.print("<tr>");
                for (int i = 0; i < value.length; i++) {
                    BindVariable v = value[i];
                    wr.print("<td><input disabled='true' type='text' value='" +
                            (v == null ? "" : v.toString()) + "' name='" + names[i] + "'/></td>");
                }
                wr.print("<td><input type='submit' onclick='var es=this.parentNode.parentNode.getElementsByTagName(\"input\");for (var i=0;i<es.length;i++) {es[i].disabled=false;}' value='>'/></td>");
                wr.print("</tr>");
            }
            wr.print("</table>");

            wr.print("<textarea rows='80' cols='80' name='sql'>");
            wr.print(replaced);
            wr.print("</textarea>");

            wr.print("</form>");*/

            wr.println("<pre>"+StringUtils.xmlEncoder(replaced,StringUtils.MASK_FULL_HTML)+"</pre>");

            wr.print("<table><tr>");
            for (String name : names) wr.print("<td>" + name + "</td>");
            wr.print("</tr>");
            for (BindVariable[] value : values) {
                wr.print("<tr>");
                for (int i = 0; i < value.length; i++) {
                    BindVariable v = value[i];
                    wr.print("<td>"+(v == null ? "" : v.toString()) + "</td>");
                }
                wr.print("</tr>");
            }
            wr.print("</table>");

        }


    }

    public String getSQLText() {
        return sql;
    }

    public List<String> getReplacementNames() {
        List<String> names = new ArrayList<String>();
        StringUtils.replaceParameters(null, null, text, replace, protect, names, true);
        return names;
    }

    public List<String> getBindVariableNames() {
        List<String> names = new ArrayList<String>();
        StringUtils.replaceParameters(null, null, text, bind, protect, names, true);
        return names;
    }

    public Map<String, String> getReplacements() {
        return replacements;
    }

    public Map<String, BindVariable> getBindVariables() {
        return values;
    }


}