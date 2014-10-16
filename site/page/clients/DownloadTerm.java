import org.w3c.dom.*;

import javax.xml.parsers.*;
import javax.xml.xpath.*;
import java.io.*;
import java.net.*;


/**
 * Sample java client to demonstrate downloading information about a GO Term
 *
 */

public class DownloadTerm {
    public static void main(String[] args) throws Exception {
        // URL a GO Term in OBO xml format
        URL u=new URL("http://www.ebi.ac.uk/QuickGO/GTerm?id=GO:0003824&format=oboxml");
        // Connect
        HttpURLConnection urlConnection = (HttpURLConnection) u.openConnection();

        // Parse an XML document from the connection
        InputStream inputStream = urlConnection.getInputStream();
        Document xml=DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(inputStream);
        inputStream.close();

        // XPath is here used to locate parts of an XML document
        XPath xpath=XPathFactory.newInstance().newXPath();

        //Locate the term name and print it out
        System.out.println("Term name:"+xpath.compile("/obo/term/name").evaluate(xml));

    }
}
