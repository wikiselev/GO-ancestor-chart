package uk.ac.ebi.interpro.exchange.compress;

import uk.ac.ebi.interpro.exchange.*;

import java.io.*;

public class CodeList {


    //public Column[] columns;
    /*public Column getColumn(String name) {
        for (Column column : columns) {
            if (column.name.equals(name))  return column;
        }
        return null;
    }*/





/*

    public static void main(String[] args) throws Exception {
        String[] s = new String[args.length - 5];
        System.arraycopy(args, 5, s, 0, s.length);
        String[] m=args[4].split(":");
        if (args[0].equals("test")) {
            encode(Integer.parseInt(args[1]), args[2], args[3], s);
            //decode(Integer.parseInt(args[1]), args[3],args[2]+".new",s,m,true);
        }
        if (args[0].equals("encode")) encode(Integer.parseInt(args[1]), args[2], args[3], s);
//        if (args[0].equals("translate")) translate();

        //if (args[0].equals("decode"))
            //decode(Integer.parseInt(args[1]), args[2],args[3],s,m,true);
        //if (args[0].equals("skip")) decode(Integer.parseInt(args[1]), args[2],args[3],s,m,false);
//        if (args[0].equals("bitcopy")) bitcopy(Integer.parseInt(args[1]),args[2],args[3]);
//        if (args[0].equals("bytecopy")) bytecopy(Integer.parseInt(args[1]),args[2],args[3]);
    }

*/


/*



    public static void decode(int limit, String fin, String fout,String[] format,String[] mask,boolean process) throws Exception {

        PrintWriter monitor = new PrintWriter(System.out, true);

        String[] columns = new String[format.length];
                for (int i = 0; i < format.length; i++) columns[i] = format[i].split(" +")[0];


        CodeList cl = new CodeList(new File(fin),format);
        DeKompress k = cl.dekompress();
        String[] data= new String[format.length];
        int[] row=new int[format.length];

        k.readTables();
        RowWriter wr;
        if (fout.length()==0)
            wr=new DiscardWriter();
        else
            wr = new TSVRowWriter(
                    new File(fout),
                    columns, true, true, monitor
            );

        int[] collect={1,2,3,4,6,8,9};
        int[][][][] info=new int[collect.length][][][];
        for (int i = 0; i < collect.length; i++) {
            int cn=collect[i];
            info[i]=new int[cl.columns[cn].count][collect.length-i-1][];
            for (int[][] valueInfo : info[i]) {
                for (int j = i+1; j < collect.length; j++) {
                    int cn2=collect[j];
                    valueInfo[j-i-1]=new int[cl.columns[cn2].count];
                }
            }
        }

        wr.open();
        int loadMask=Integer.parseInt(mask[0],2);
        int testMask=Integer.parseInt(mask[1],2);
        int getMask=Integer.parseInt(mask[2],2);
        long time=System.nanoTime();
        int ct=0;
        //Bitmap bitmap=cl.columns[1].bitmaps.get("SPKW");

        while (ct<limit) {

            //if (!k.skip((int) bitmap.interval.data[i++])) break;
            if (!k.read(row)) break;
            for (int j = 0; j < collect.length; j++) {
                int[][] valueInfo = info[j][row[collect[j]]];
                for (int m = j+1; m < collect.length; m++) {
                    valueInfo[m-j-1][row[collect[m]]]++;
                }

            }
            //if (process) wr.write(row);
            ct++;
        }
        time=System.nanoTime()-time;
        k.close();
        wr.close();
        System.out.println(ct+" rows "+time/1000000+" ms "+time/k.loaded+" ns/row");
        for (int i=0;i<collect.length;i++) {
            CodeList.Column column = cl.columns[collect[i]];
            System.out.println("Categorization:"+ column.name);
            for (int j = 0; j < info[i].length; j++) {
                //System.out.println(column.name+"="+column.valueList[j]);
                for (int m=i+1;m<collect.length;m++) {
                    CodeList.Column c2 = cl.columns[collect[m]];
                    System.out.println("SubCategorization:"+ c2.name);
                    for (int n = 0; n < info[i][j][m-i-1].length; n++) {
                        int vc = info[i][j][m - i - 1][n];
                        //if (vc!=0) System.out.println("Value:"+c2.valueList[n]+" "+ vc);


                    }
                }
            }
        }

    }

*/
    /*public static void encode(int limit, String fin, String fout, String[] format) throws Exception {

        PrintWriter monitor = new PrintWriter(System.out, true);

        CodeList cl = new CodeList(new File(fout),format);
        Kompress k = cl.kompress();

        String[] columns = new String[format.length];
        for (int i = 0; i < format.length; i++) columns[i] = format[i].split(" +")[0];

        String[] row = new String[format.length];


        RowReader rd = new Progress(new NVL(new Head(limit, new TSVRowReader(
                new File(fin),
                columns, true, true, monitor))));

        rd.open();
        while (rd.read(row)) {
            //System.out.println(">>>"+row[0]+" "+row[1]+" "+row[2]);
            k.add(row);
        }
        rd.close();
        rd.open();
        k.writeTables();
        long time=System.nanoTime();
        int ct=0;
        while (rd.read(row) && ct<limit) {
            ct++;
            if (ct%10000==0) System.out.print(".");
            k.write(row);
        }
        time=System.nanoTime()-time;
        k.close();
        rd.close();

        System.out.println(ct+" rows "+time/1000000+" ms "+time/ct+" ns/row");
    }*/

    private static class DiscardWriter implements RowWriter {
        public void write(String[] data) throws Exception {

        }

        public void open() throws Exception {

        }

        public void close() throws Exception {

        }

        public void setColumns(String[] columns) throws Exception {

        }
    }

    public static class NVL implements RowReader.MonitorRowReader {
        MonitorRowReader underlying;

        public NVL(MonitorRowReader underlying) {this.underlying = underlying;}
        public boolean read(String[] data) throws Exception {
            if (!underlying.read(data)) return false;
            for (int i = 0; i < data.length; i++) {if (data[i] == null) data[i] = "";}
            return true;
        }

        public void open() throws Exception {underlying.open();}
        public void close() throws Exception {underlying.close();}
        public String[] getColumns() throws Exception {return underlying.getColumns();}

        public double getFraction() {
            return underlying.getFraction();
        }
    }
/*

    public static class Progress implements RowReader {
            RowReader underlying;
        int ct=0;
            public Progress(RowReader underlying) {this.underlying = underlying;}
            public boolean read(String[] data) throws Exception {
                boolean r = underlying.read(data);
                if (r) ct++;
                if (ct%10000==0) System.out.print("-");
                return r;

            }

            public void open() throws Exception {underlying.open();ct=0;}
            public void close() throws Exception {
                underlying.close();
                System.out.println(":"+ct);
            }
            public String[] getColumns() throws Exception {return underlying.getColumns();}
        }
*/

    public static class Head implements RowReader.MonitorRowReader {

        private int limit;
        RowReader underlying;
        int rc;

        public Head(int limit, RowReader underlying) {
            this.limit = limit;
            this.underlying = underlying;
        }

        public boolean read(String[] data) throws Exception {return rc++ < limit && underlying.read(data);}
        public void open() throws Exception {
            rc = 0;
            underlying.open();
        }

        public void close() throws Exception {underlying.close();}
        public String[] getColumns() throws Exception {return underlying.getColumns();}


        public double getFraction() {
            return rc/limit;
        }
    }

}
