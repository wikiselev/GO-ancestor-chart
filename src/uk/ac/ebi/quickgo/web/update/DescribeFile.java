package uk.ac.ebi.quickgo.web.update;

import uk.ac.ebi.interpro.exchange.compress.*;
import uk.ac.ebi.interpro.common.collections.*;

import java.io.*;
import java.util.*;

public class DescribeFile {

    static String leBitString(long code, int size) {
        StringBuilder sb = new StringBuilder();
        while (size > 0) {
            sb.append(code & 1);
            size--;
            code >>= 1;
        }
        return sb.toString();
    }

    static String beBitString(long code, int size) {
        StringBuilder sb = new StringBuilder();
        while (size > 0) {
            sb.insert(0,code & 1);
            size--;
            code >>= 1;
        }
        return sb.toString();
    }

    public static void main(String[] args) throws Exception {
        if (args.length<=0) {

            System.out.println("java -jar QuickGO5.1.jar describe filename command...");
            System.out.println("QuickGO data file inspection tool");
            System.out.println("");
            System.out.println("anyfile dump tp:start:bits:count");
            System.out.println("  Dump bit aligned region(s) of file. Multiple regions may be supplied");
            System.out.println("  tp: format for each unit read. c=character n=decimal b=binary (little endian)");
            System.out.println("  start: bit index into file to start reading");
            System.out.println("  bits: number of bits to read for each unit");
            System.out.println("  count: number of units to read");
            System.out.println("");
            System.out.println("Specific file types supported");
            System.out.println("*.ils integer table");
            System.out.println("*.ind index");
            System.out.println("*.tls text list");
            System.out.println("  for each file type, commands will be printed if not supplied");
            System.exit(1);
        }
        List<Closeable> connection=new ArrayList<Closeable>();
        String what = args.length > 2 ? args[2] : null;
        String command = args.length>1?args[1]:null;
        File file = new File(args[0]);
        String type=file.getName().replaceFirst(".*\\.","");

        if ("dump".equals(command)) {
            BitReader br=new BitReader(file);
            int i=2;
            while (i<args.length) {
                String[] text=args[i].split(":");
                String how=text[0];
                br.seek(Long.parseLong(text[1]));
                int bitsize = text.length<3?32:Integer.parseInt(text[2]);
                int count = text.length<4?1:Integer.parseInt(text[3]);
                for (int j=0;j< count;j++) {
                    long v=br.readLong(bitsize);
                    if (how.equals("c")) System.out.print((v<32?("#"+v):(char)v)+" ");
                    if (how.equals("n")) System.out.print(v+" ");
                    if (how.equals("b")) System.out.print(leBitString(v,bitsize)+" ");
                }
                System.out.println();
                i++;
            }
            br.close();

            return;
        }

        if (type.equals("ind")) {
            IndexReader rd = new IndexReader(file);




            if ("list".equals(command)) {
                IndexReader.ValueRead vr = rd.open(connection, Integer.parseInt(what));
                int at = 0;
                while ((at = vr.next(at))!=Integer.MAX_VALUE) {
                    System.out.println(at);
                    at++;
                }
            } else if ("analyze".equals(command)) {


                /*long totalSize=0;
                int[] bitSizeCounts=new int[32];
                int[] totalBitSizeCounts=new int[32];
                int[] largest=new int[32];
                int[] largestCounts=new int[32];

                int upper=what==null?0:Integer.parseInt(what);
                int saving=0;

                IndexReader.ValueRead vr=
               for (int i=0;i<rd.size();i++) {
                   vr.seek(rd.values[i]);
                   int bitSize=vr.bitSize;
                   bitSizeCounts[bitSize]++;
                   vr.count();
                   int totalbitsize = vr.bitSize * vr.count();
                   totalSize+= totalbitsize;
                   if (totalbitsize>upper) saving+=(totalbitsize-upper);
                   totalBitSizeCounts[bitSize]+=vr.count();
                   if (vr.count()>=largestCounts[bitSize]) {
                       largestCounts[bitSize]=vr.count();
                       largest[bitSize]=i;
                   }
               }
                 System.out.println("Saving:"+saving);
                System.out.println("Total bit size:"+totalSize);
                System.out.println("Bits : count : values : largest : values");
                for (int i = 0; i < totalBitSizeCounts.length; i++) {
                    System.out.println(i+" : "+bitSizeCounts[i]+" : "+totalBitSizeCounts[i]+" : "+largest[i]+" : "+largest[i]);

                }
                vr.close();*/
            } else if ("info".equals(command)) {
                IndexReader.ValueRead vr = rd.open(connection, Integer.parseInt(what));

                System.out.println(vr.toString());

            } else {

                System.out.println("*.ind list code");
                System.out.println("  List hits for an index at a code");
                System.out.println("*.ind info code");
                System.out.println("  Print info about the index at a code");
                System.out.println("Index "+rd.from+" -> "+rd.to);
                for (Map.Entry<String, String> info : rd.debugInfo.entrySet())
                    System.out.println(info.getKey()+" : "+info.getValue());


            }

        }

        if (type.equals("tls")) {
            TextTableReader tr = new TextTableReader(file);

            if ("list".equals(command)) {
                TextTableReader.Cursor tc=tr.open(connection);
                String[] value;
                while ((value=tc.read())!=null) {
                    System.out.println(CollectionUtils.concat(value,"\t"));
                }
                tc.close();
            } else if ("find".equals(command)) {
                TextTableReader.Cursor tc=tr.open(connection);
                System.out.println(tc.search(what.split(",")));
            } else if ("prefix".equals(command)) {
                TextTableReader.Cursor tc=tr.open(connection);
                String[] row = what.split(",");
                int index = tc.search(row,true);
                tc.seek(index);
                String[] value;
                while ((value=tc.nextPrefix(row))!=null) {
                    System.out.println(CollectionUtils.concat(value,"\t"));
                }

            } else if ("get".equals(command)) {
                System.out.println(CollectionUtils.concat(tr.open(connection).read(Integer.parseInt(what)),"\t"));
            } else {
                System.out.println("*.tls list");
                System.out.println("  List all the rows in table, tab separated");
                System.out.println("*.tls find text");
                System.out.println("  Find the code corresponding to supplied text (comma separated)");
                System.out.println("*.tls get code");
                System.out.println("  Get the text row corresponding to a code, tab separated");
                System.out.println("Table "+tr.rowType);
                System.out.println("");
                for (Map.Entry<String, String> info : tr.debugInfo.entrySet())
                    System.out.println(info.getKey()+" : "+info.getValue());
            }


        }

        if (type.equals("ils")) {
            IntegerTableReader tr = new IntegerTableReader(file);

            if ("list".equals(command)) {
                IntegerTableReader.Cursor tc=tr.open(connection);
                int[] value;
                while ((value=tc.read())!=null) {
                    System.out.println(CollectionUtils.concat(value,"\t"));
                }
                tc.close();
            } else if ("find".equals(command)) {
                IntegerTableReader.Cursor tc=tr.open(connection);


                    String[] t=what.split(",");
                    int[] v=new int[t.length];
                    for (int i=0;i<t.length;i++) v[i]=Integer.parseInt(t[i]);
                    System.out.println(tc.search(v));

            } else if ("get".equals(command)) {
                System.out.println(CollectionUtils.concat(tr.open(connection).read(Integer.parseInt(what)),"\t"));
            } else {
                System.out.println("*.ils list");
                System.out.println("  List all the rows in table, tab separated");
                System.out.println("*.ils find value");
                System.out.println("  Search for the code corresponding to supplied value (comma separated)");
                System.out.println("*.ils get code");
                System.out.println("  Get the integer row corresponding to a code, tab separated");
                System.out.println("Table "+tr.columnCount()+" "+tr.rowCount());

                System.out.println("Row type: "+tr.rowType);
                for (int i=0;i<tr.valueTypes.length;i++) {
                    System.out.println("Column "+i+": "+tr.valueTypes[i]);
                }
            }


        }

    }
}
