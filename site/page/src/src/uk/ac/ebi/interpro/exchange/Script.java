package uk.ac.ebi.interpro.exchange;

import uk.ac.ebi.interpro.common.*;
import uk.ac.ebi.interpro.common.collections.*;
import uk.ac.ebi.interpro.common.database.*;

import javax.sql.*;
import java.io.*;
import java.net.*;
import java.sql.*;
import java.text.*;
import java.util.*;
import java.util.Date;
import java.util.regex.*;


public class Script {

    //Map<String,String> props=new HashMap<String, String>();

    SimpleDateFormat timeStampFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z ");

    PrintWriter out = new PrintWriter(System.out) {
        boolean newLine = true;

        public void write(String string) {
            if (newLine)
                super.write(timeStampFormat.format(new Date()));
            newLine = false;
            super.write(string);
            flush();
        }

        public void println() {
            super.println();
            flush();
            newLine = true;
        }


    };

    File base;



    DataSourceCollection db = new DataSourceCollection();

    boolean ignore=false;
    int sqlerrorExitCode=1;

    public class Config {
        String erase="delete from $table";
        String insert="insert into $table ($columns) values ($values)";

        boolean savepoint;

         Config(Map<String,String> props) {
            erase=StringUtils.nvl(props.get("erase"),erase);
            insert=StringUtils.nvl(props.get("insert"),insert);

            savepoint=props.get("savepoint")!=null;
        }
    }



    public static void erase(Connection target, String table,Config config,PrintWriter out) throws SQLException {
        out.print("Emptying: " + table);
        PreparedStatement delete;
        delete = target.prepareStatement(config.erase.replace("$table",table));
        delete.executeUpdate();
        delete.close();
        out.println("Done");
    }

/*
    private static void readRow(ResultSet rset,String[] data) throws SQLException {
        for (int i = 0; i < data.length; i++) {
            data[i] = rset.getString(i + 1);

        }
    }
*/

    public static String suckClob(Clob clob) throws IOException, SQLException {
        return IOUtils.readStringClose(clob.getCharacterStream());
    }


    static class JDBCRowReader implements RowReader {
        private String selectSQL;
        private Connection from;

        public String toString() {
            return description;
        }

        ResultSet rset;
        PreparedStatement select;
        int nc;
        private String description;
        private PrintWriter monitor;
        int[] types;

        public JDBCRowReader(Connection from, String selectSQL, String description,PrintWriter monitor) {
            this.selectSQL = selectSQL;
            this.from = from;
            //out.println("Querying:" + selectSQL);
            this.description = description;
            this.monitor = monitor;



        }


        public boolean read(String[] data) throws Exception {
            if (!rset.next()) return false;
            for (int i = 0; i < data.length; i++) {
                data[i] = types[i]==Types.CLOB?suckClob(rset.getClob(i+1)):rset.getString(i + 1);

            }

            return true;
        }



        public void open() throws SQLException {
            select = from.prepareStatement(selectSQL);
            rset = select.executeQuery();
            ResultSetMetaData rmd = rset.getMetaData();
            nc = rmd.getColumnCount();
            types=new int[nc];
            for (int i = 0; i < nc; i++) types[i]=rmd.getColumnType(i+1);
            monitor.println("Read JDBC: "+selectSQL);
        }

        public void close() throws Exception {
            select.close();
        }

        public String[] getColumns() throws SQLException {
            String[] cn=new String[nc];

            for (int i = 0; i < cn.length; i++) {
                ResultSetMetaData rmd = rset.getMetaData();
                cn[i]=rmd.getColumnName(i+1);


            }
            return cn;
        }
    }




    static class JDBCRowWriter implements RowWriter {

        JDBCRowSQLWriter insertWriter;
        private Connection to;
        private String toTable;
        private String[] columns;
        private Config config;

        private String description;
        private PrintWriter monitor;


        public String toString() {
            return description;
        }


        public JDBCRowWriter(Connection to, String toTable, String[] columns, Config config,String description,PrintWriter monitor) {
            this.to = to;
            this.toTable = toTable;
            this.columns = columns;
            this.config = config;

            this.description = description;
            this.monitor = monitor;
        }

        public void setColumns(String[] columns) throws Exception {
            if (this.columns==null) this.columns=columns;
        }

        public void open() throws SQLException {

            erase(to, toTable,config, monitor);

            StringBuffer bind = new StringBuffer();
            for (int i = 0; i < columns.length; i++) {
                if (i != 0) bind.append(",");
                bind.append("?");
            }


            String clist=CollectionUtils.concat(Arrays.asList(columns),",");

            insertWriter=new JDBCRowSQLWriter(to,
                    config.insert.replace("$table",toTable).replace("$columns",clist).replace("$values",bind),
                    description,monitor);
            insertWriter.open();
        }

        public void write(String[] data) throws Exception {
            insertWriter.write(data);
        }

        public void close() throws Exception {
            insertWriter.close();
        }


    }

    static class JDBCRowSQLWriter implements RowWriter {
        private Connection to;
        private String sql;
        private String description;
        private PrintWriter out;


        public String toString() {
            return description;
        }

        PreparedStatement insert;
        int rc = 0;

        public JDBCRowSQLWriter(Connection to, String sql, String description,PrintWriter out) {
            this.to = to;
            this.sql = sql;
            this.description = description;
            this.out = out;
        }

        public void setColumns(String[] columns) throws Exception {
            out.println("Write columns:"+CollectionUtils.concat(columns,","));
        }

        public void open() throws SQLException {
            out.println("Write JDBC: "+sql);
            insert = this.to.prepareStatement(sql);
        }

        public void write(String[] data) throws Exception {
            for (int i = 0; i < data.length; i++) {
                insert.setString(i + 1, data[i]);
            }
            insert.addBatch();
            if ((rc++) % 1000 == 0) insert.executeBatch();


        }

        public void close() throws Exception {
            insert.executeBatch();
            insert.close();
        }


    }


    public static int[] findColumnIndexes(String[] seek,String[] source) throws IOException {
        int[] index=new int[seek.length];
        for (int i = 0; i < seek.length; i++) {

            index[i] = -1;
            for (int j = 0; j <source.length; j++)
                if (source[j].equals(seek[i])) index[i] = j;
            if (index[i]==-1) throw new IOException("Column "+seek[i]+" not found");
        }
        return index;
    }


    void copy(RowReader from, RowWriter to) throws Exception {
        out.print("Copying");
        from.open();
        String[] columns = from.getColumns();
        to.setColumns(columns);
        to.open();

        String[] data=new String[columns.length];
        long start = System.currentTimeMillis();
        int rc = 0;
        while (from.read(data)) {
            to.write(data);
            if (rc % 50000 == 0) out.print(".");
            rc++;
        }
        long end = System.currentTimeMillis();
        from.close();
        to.close();
        out.println("Completed");

        out.println((rc==0?"no data ":rc + " rows " + (end - start) * 1000 / rc + " us/row ") + (end - start) / 1000 + "seconds");
    }

    void executeSQL(Connection target, String command) throws SQLException {
        out.println(command);
        PreparedStatement stmt = target.prepareStatement(command);
        stmt.execute();
        stmt.close();

    }


    class ScriptParseException extends Exception {
        public ScriptParseException(String string) {
            super(string);
        }
    }

    Stack<RowReader> rowReader=new Stack<RowReader>();
    Stack<RowWriter> rowWriter=new Stack<RowWriter>();
    Config config;
    int version=0;

    Pattern space=Pattern.compile(" +");

    String[] readArgs(StringBuilder command,int count) {
        String[] text=new String[count];
        for (int i=0;i<text.length;i++) {
            text[i]=readUntil(command,space);
        }
        return text;
    }

    String readUntil(StringBuilder command, Pattern terminal) {
        Matcher matcher=null;
        if (terminal!=null) {
            matcher=terminal.matcher(command);
            if (!matcher.find()) matcher=null;
        }
        String s = matcher==null?command.toString():command.substring(0, matcher.start());
        if (matcher==null) command.setLength(0); else command.delete(0,matcher.end());
        return s;
    }


    void execCommand(Connection target, String line) throws Exception {
        StringBuilder parse=new StringBuilder(line);
        String command=readUntil(parse,space);

            if (command.equals("script")) {
                version=Integer.parseInt(readArgs(parse,1)[0]);
            } else if (command.equals("whenever")) {
                String[] symbol = readArgs(parse,4);
                if (symbol[1].equals("continue")) {
                    ignore=true;
                } else if (symbol[1].equals("exit")) {
                    ignore=false;
                    sqlerrorExitCode=Integer.parseInt(symbol[3]);
                } else {
                    throw new Exception("whenever sqlerror { continue | exit n}");
                }
            } else if (command.equals("write")) {
                String[] symbol = readArgs(parse,3);

                String[] columns = symbol[0].equals("*")?null:symbol[0].split(",");
                String format=symbol[1];
                String fileName = symbol[2];

                if (format.startsWith("postgres") || format.startsWith("tab"))
                    rowWriter.push(new TSVRowWriter(IOUtils.relativeFile(base, fileName), columns, format.startsWith("postgres"),format.endsWith("+gz"),out));
                else throw new Exception("Format ("+format+") unknown");
            } else if (command.equals("read")) {
                String[] symbol = readArgs(parse,3);

                String[] columns = symbol[0].equals("*")?null:symbol[0].split(",");
                String format=symbol[1];
                String fileName = symbol[2];

                if (format.startsWith("postgres") || format.startsWith("tab"))
                    rowReader.push(new TSVRowReader(IOUtils.relativeFile(base, fileName), columns, format.startsWith("postgres"),format.endsWith("+gz"),out));
                else throw new Exception("Format ("+format+") unknown");
            } else if (command.equals("store")) {
                rowWriter.push(new JDBCRowSQLWriter(target, parse.toString(),"write",out));
            } else if (command.equals("insert") && version==0) {
                rowWriter.push(new JDBCRowSQLWriter(target, line,"write",out));                                
            } else if (command.equals("replace")) {
                String[] symbol = readArgs(parse,2);
                String[] columns = symbol[0].equals("*")?null:symbol[0].split(",");
                String table = symbol[1];
                rowWriter.push(new JDBCRowWriter(target, table, columns, config,table,out));
            } else if (command.equals("load")) {
                rowReader.push(new JDBCRowReader(target, parse.toString(),"read",out));
            } else if (command.equals("select")) {
                rowReader.push(new JDBCRowReader(target, line,"read",out));
            } else if (command.equals("copy")) {
                copy(rowReader.pop(),rowWriter.pop());
            } else if (command.equals("compare")) {
                RowReader current = rowReader.pop();
                RowReader previous = rowReader.pop();

                String[] columns = readUntil(parse,space).split(",");

                RowWriter diff = rowWriter.pop();

                //compare(current, previous,store, diff,columns);
                compare(new OrderCheckReader(previous,columns),
                        new OrderCheckReader(current,columns),
                        columns,new Diff(diff));
            } else if (command.equals("merge")) {

                String[] columns = readUntil(parse,space).split(",");

                RowWriter destination = rowWriter.pop();
                RowReader old = rowReader.pop();
                RowReader diff = rowReader.pop();
                //merge(diff, old, destination,columns);
                compare(new OrderCheckReader(old,columns),
                        new OrderCheckReader(diff,columns),
                        columns,new Merge(destination));

            } else if (command.equals("check")) {
                String[] symbol=readArgs(parse,2);
                if (new File(symbol[0]).length()<Long.parseLong(symbol[1])) {
                    out.println("File "+symbol[0]+" less than "+symbol[1]+" bytes. Aborting.");
                    System.exit(3);
                }
            } else if (command.equals("execute")) {
                executeSQL(target, parse.toString());
            } else if (command.equals("may")) {
                String sql = parse.toString();
                mayExecute(target, sql);
            } else {
                if (ignore) mayExecute(target, line);
                else executeSQL(target, line);
            }

    }

    private void mayExecute(Connection target, String sql) throws SQLException {
        Savepoint sp=config.savepoint?target.setSavepoint():null;
        try {

            executeSQL(target, sql);
        } catch (SQLException e) {
            if (sp!=null) target.rollback(sp);
            out.println("Ignoring: "+e.getMessage());
        }
    }

    interface CompareAction {
        void open(int leftColumns,int rightColumns) throws Exception;
        void close() throws Exception;
        void left(String[] row) throws Exception;
        void right(String[] row) throws Exception;
        void common(String[] left,String[] right) throws Exception;
    }

    class Diff implements CompareAction {

        RowWriter diff;

        String[] d;
        private int[] identity;


        public Diff(RowWriter diff) throws Exception {
            this.diff = diff;
        }

        public void close() throws Exception {
            diff.close();
        }

        public void open(int lc,int rc) throws Exception {
            diff.open();
            d=new String[lc+1];
            identity=new int[rc];
            for (int i=0;i<identity.length;i++) identity[i]=i;
        }

        public void left(String[] row) throws Exception {
            System.arraycopy(row,0,d,0,row.length);
            d[d.length-1]="D";
            diff.write(d);

        }

        public void right(String[] row) throws Exception {
            System.arraycopy(row,0,d,0,row.length);
            d[d.length-1]="I";
            diff.write(d);
        }

        public void common(String[] left, String[] right) throws Exception {
            if (compare(left,right,identity)==0) return;
            System.arraycopy(right,0,d,0,right.length);
            d[d.length-1]="U";
            diff.write(d);
        }
    }

    class Merge implements CompareAction {

        RowWriter target;

        String[] t;

        public Merge(RowWriter target) {
            this.target = target;
        }

        public void open(int leftColumns, int rightColumns) throws Exception {
            target.open();
            t=new String[leftColumns];
        }

        public void close() throws Exception {
            target.close();
        }

        public void left(String[] row) throws Exception {
            target.write(row);
        }

        public void right(String[] row) throws Exception {
            if (!"D".equals(row[row.length-1])) {// should always be I
                System.arraycopy(row,0,t,0,t.length);
                target.write(t);
            }
        }

        public void common(String[] left, String[] right) throws Exception {
            if (!"D".equals(right[right.length-1])) {// might be D or U
                System.arraycopy(right,0,t,0,t.length);
                target.write(t);

            }
        }
    }

    class OrderCheckReader implements RowReader {
        private RowReader source;
        private String[] keyColumns;
        String[] previous;
        int[] indexes;


        public OrderCheckReader(RowReader source,String[] keyColumns) {
            this.source = source;
            this.keyColumns = keyColumns;
        }

        public boolean read(String[] data) throws Exception {
            boolean underlying=source.read(data);
            if (previous==null) {
                indexes=findColumnIndexes(keyColumns,source.getColumns());
                previous=new String[data.length];
            } else {
                if (compare(previous,data,indexes)>0) {
                    throw new IOException("Rows not in order");
                }
            }
            System.arraycopy(data,0,previous,0,data.length);
            return underlying;
        }

        public void open() throws Exception {source.open();}

        public void close() throws Exception {source.close();}

        public String[] getColumns() throws Exception {return source.getColumns();}
    }

    class TeeReader implements RowReader {
        private RowReader source;
        private RowWriter log;

        TeeReader(RowReader source,RowWriter log) {
            this.source = source;
            this.log = log;
        }

        public boolean read(String[] data) throws Exception {
            boolean hasData=source.read(data);
            log.write(data);
            return hasData;
        }

        public void open() throws Exception {
            source.open();
            log.open();
        }

        public void close() throws Exception {
            source.close();
            log.close();
        }

        public String[] getColumns() throws Exception {return source.getColumns();}
    }


    public void compare(RowReader left,RowReader right,String[] keyColumns,CompareAction action) throws Exception {

        left.open();
        right.open();

        String[] lc=left.getColumns();
        String[] rc=right.getColumns();
        String[] l=new String[lc.length];
        String[] r=new String[rc.length];
        int[] lci=findColumnIndexes(keyColumns,lc);
        int[] rci=findColumnIndexes(keyColumns,rc);

        action.open(lc.length,rc.length);
        if (!left.read(l)) l=null;
        if (!right.read(r)) r=null;

        while (l!=null || r!=null) {

            int cf=compare(l,r,lci,rci);

            if (cf==0) action.common(l,r);
            if (cf<0) action.left(l);
            if (cf>0) action.right(r);
            if (cf<=0) if (!left.read(l)) l=null;
            if (cf>=0) if (!right.read(r)) r=null;
        }

        action.close();
        left.close();
        right.close();
    }

    public int compare(String[] a,String[] b,int[] commonIndex) {
        return compare(a,b,commonIndex,commonIndex);
    }

    public int compare(String[] a,String[] b,int[] aIndex,int[] bIndex) {
        if (a==null && b==null) return 0;
        if (a==null) return 1;
        if (b==null) return -1;
        for (int k = 0; k < aIndex.length; k++) {
            int c=a[aIndex[k]].compareTo(b[bIndex[k]]);
            if (c!=0) return c;
        }
        return 0;
    }



    /*void merge(Connection target,String selectSQL,RowReader diff,String[] keyColumns) throws Exception {
        ResultSet rset=target.prepareStatement(selectSQL).executeQuery();

        String[] columnNames=diff.getColumns();
        String[] d=new String[columnNames.length];

        boolean sqldata=rset.next();
        diff.read(d);
        String[] r=new String[rset.getMetaData().getColumnCount()];
        readRow(rset, r);
        int[] pkc=findColumnIndexes(keyColumns,columnNames);


        while (sqldata || d!=null) {

            String action=d==null?"":d[d.length-1];

            if (compare(r,d,pkc)==0) {

                if (action.equals("D")) {
                    rset.deleteRow();
                }
                if (action.equals("U")) {
                    updateRow(rset,d,r.length);
                    rset.updateRow();
                }
            }

            if (action.equals("I")) {
                rset.moveToInsertRow();
                updateRow(rset,d,r.length);
                rset.insertRow();
                rset.moveToCurrentRow();
            }

            if (compare(r,d,pkc)<=0) {
                readRow(rset, r);
            }

            if (compare(r,d,pkc)>=0) {
                diff.read(d);
            }

        }
    }*/

    /*private void updateRow(ResultSet rset, String[] data, int length) throws SQLException {
        for (int i=0;i<length;i++) rset.updateString(i+1,data[i]);

    }
*/
    void execScript(Connection target, File script) throws Exception {
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(script)));
        String line;
        StringBuilder buffer = new StringBuilder();
        while ((line = br.readLine()) != null) {
            String trimmed = line.trim();
            if (trimmed.startsWith("--")) continue;
            if (trimmed.endsWith(";")) {
                buffer.append(trimmed.substring(0, trimmed.length() - 1));
                execCommand(target, buffer.toString().trim());
                buffer.setLength(0);
            } else {
                buffer.append(line).append(" ");
            }
        }
        br.close();


    }


    public void diagnose(Exception e) {
        if (e == null) return;
        e.printStackTrace(out);
        if (e instanceof SQLException) {
            diagnose(((SQLException) e).getNextException());
        }
    }

    public static void main(String[] args) {





        String configFile=null;
        List<String> command=new ArrayList<String>();
        String dataDir=null;
        String scriptFile=null;

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.equals("-c")) configFile=args[++i];
            else if (arg.equals("-f")) scriptFile=args[++i];
            else if (arg.equals("-e")) command.add(args[++i]);
            else if (arg.equals("-d")) dataDir=args[++i];
            else System.out.println("Unknown parameter "+arg);
        }

        if (command.isEmpty() && scriptFile==null) {
            System.out.println("Parameters: [-c database-config-file] [ -d <data-directory> ] ( -f script-file | -e command -e command ... )");
            System.out.println("command can be any of the following commands (which may need to be quoted appropriately for your shell)");
            System.out.println("script-file should contain lines");
            System.out.println("-- comments like this");
            System.out.println("  <sql command> ;\n" +
                    "-- will execute command. If it fails it will rollback and exit.\n"+
                    "-- commands may be on multiple lines and are terminated by a semicolon");
            System.out.println("  may <sql command> ;\n" +
                    "-- will execute command, ignoring failure");
//            System.out.println("\n" +
//                    "  from select <statement>\n" +
//                    "  from file <format> <filename>\n" +
//                    "  to table <tablename>\n" +
//                    "  to file <format> <filename>");
            System.out.println("  load <format> <file> <header1,header2,..> <table> <column1,column2,...> ;\n" +
                    "-- will load a database table from a file, truncating the table first.");
            System.out.println("  store <format> <file> <header1,header2,..> <select statement> ;\n" +
                    "-- will store the results of a query in a file");
            System.out.println("  check <file> <minimum size> ;\n" +
                    "-- will check that the file is of the specified size (and abort if not)");
            System.out.println("\n" +
                    "<format> is one of:\n" +
                    "postgres      PostgreSQL dump file format, tab separated, backslash quoted\n" +
                    "tab           Plain tab separated, no quoting (so tabs, newlines and nulls will be lost)\n" +
                    "binary        Heavily compressed custom binary format\n" +
                    "<format>+gz   GZip format"
            );
            System.out.println("");
            System.out.println("database-config-file is a properties file:");
            System.out.println(DataSourceCollection.syntaxDriverDataSource);
            System.exit(1);
        }

        int code=0;

        Script script = new Script();
        Connection c=null;
        try {
            if (dataDir!=null)
                script.base = new File(dataDir);

            if (configFile!=null) {
                DataSourceCollection dsc=new DataSourceCollection();
                URL url = new File(configFile).toURL();
                Map<String, String> props = CollectionUtils.loadPropertyMap(url);
                DataSource ds=dsc.get(url,props);
                c=ds.getConnection();
                script.configure(CollectionUtils.keyFilter(props,CollectionUtils.removePrefix("exchange.")));
            }



            if (scriptFile!=null)
                script.execScript(c, new File(scriptFile));
            else if (!command.isEmpty()) {
                for (int i = 0; i < command.size(); i++) script.execCommand(c,command.get(i));
            }
            if (c!=null) c.commit();
        } catch (ScriptParseException e) {
            script.out.println("Syntax Error: "+e.getMessage());
            code=1;
            //script.diagnose(e);
        } catch (SQLException e) {
            script.out.println("SQL Error: "+e.getMessage());
            code=script.sqlerrorExitCode;
            //script.diagnose(e);
        } catch (Exception e) {
            script.diagnose(e);
            code=1;
        } finally {
            if (c!=null) {
                try {
                    c.close();
                } catch (SQLException e) {
                    script.out.println("Connection close failed: "+e.getMessage());
                }
            }
        }
        script.out.flush();

        System.exit(code);



    }

    private void configure(Map<String, String> props) {
        config=new Config(props);
    }
}
