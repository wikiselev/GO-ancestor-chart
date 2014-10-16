import java.net.*;
import java.io.*;
import java.util.*;

/**
 * Sample java client to demonstrate downloading an annotation set
 * for a protein and retrieving the unique GO terms from it.
 *
 */

class DownloadAnnotation {
    public static void main(String[] args) throws IOException {
        // URL for annotations from QuickGO for one protein
        URL u=new URL("http://www.ebi.ac.uk/QuickGO/GAnnotation?protein=P12345&format=tsv");
        // Connect
        HttpURLConnection urlConnection = (HttpURLConnection) u.openConnection();
        // Get data
        BufferedReader rd=new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
        // Read data
        List<String> columns=Arrays.asList(rd.readLine().split("\t"));
        System.out.println(columns);
        // Collect the unique terms as a sorted set
        Set<String> terms=new TreeSet<String>();
        // Find which column contains GO IDs
        int termIndex=columns.indexOf("GO ID");
        // Read the annotations line by line
        String line;
        while ((line=rd.readLine())!=null) {
            // Split them into fields
            String[] fields=line.split("\t");
            // Record the GO ID
            terms.add(fields[termIndex]);
        }
        // close input when finished
        rd.close();
        // Write out the unique terms
        for (String term : terms) {
            System.out.println(term);
        }
    }
}