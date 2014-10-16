package uk.ac.ebi.interpro.common.database;


import org.w3c.dom.*;
import org.xml.sax.*;
import uk.ac.ebi.interpro.common.*;

import javax.xml.parsers.*;
import java.io.*;
import java.net.*;
import java.util.*;

public class QuerySet {

    public Map<String, String> config = new HashMap<String, String>();
    private Seeker replace = SQLCommand.ampWord;
    private Seeker bind = SQLCommand.colonWord;
    private Seeker protect = SQLCommand.singleQuote;


    public QuerySet(Map<String, String> cfg) {
        config.putAll(cfg);
    }

    public QuerySet(URL xmlFormatQueries) throws IOException, ParserConfigurationException, SAXException {
        this(load(xmlFormatQueries));
    }

    public static Element load(URL xmlFormatQueries) throws IOException, SAXException, ParserConfigurationException {
        InputStream is = xmlFormatQueries.openStream();
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(is);
        is.close();
        return doc.getDocumentElement();
    }

    public QuerySet(Element root) {
        NodeList elts = root.getElementsByTagName("sql");

        for (int i = 0; i < elts.getLength(); i++) {
            Element elt = (Element) elts.item(i);
            config.put(elt.getAttribute("name"), elt.getFirstChild().getNodeValue());
            //System.out.println("["+elt.getAttribute("name")+"]["+elt.getFirstChild().getNodeValue()+"]");
        }
        replace = StringUtils.nvl(SQLCommand.make(config.get("replace")), SQLCommand.dollarCurly);
        bind = StringUtils.nvl(SQLCommand.make(config.get("bind")), SQLCommand.colonWord);
        protect = StringUtils.nvl(SQLCommand.make(config.get("protect")), SQLCommand.singleQuote);
    }

    public SQLCommand get(String name) {
        String text = config.get(name);
        if (text == null) throw new NullPointerException("No such sql statement " + name);

        SQLCommand command = new SQLCommand(name, text).setReplacements(config);

        command.setFormat(replace, bind, protect);
        command.formLocation = config.get("location");
        return command;
    }

    

}
