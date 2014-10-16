package uk.ac.ebi.quickgo.web.configuration;

import org.w3c.dom.*;
import uk.ac.ebi.quickgo.web.*;
import uk.ac.ebi.quickgo.web.graphics.*;
import uk.ac.ebi.interpro.common.xml.*;

import javax.xml.parsers.*;
import java.io.*;

public class Configuration {
    public long lastModified;
    public long loaded;
    public Pages pages;
        
    public Element root;
    public File base;
    public Feedback feedback;
    public ImageArchive imageArchive=new ImageArchive();
    public Features features;
	public Databases databases;
    //public DataManager dataManager;
    public String id;
    public String password;
    public ChartControl chartControl;
	public StatsControl statsControl;

    public Configuration(QuickGO quickGO,String configFile) {
        try {
            File file = new File(configFile);
            base = file.getParentFile();

            loaded = System.currentTimeMillis();
            lastModified = file.lastModified();
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();

            Document document = builder.parse(file);
            root = document.getDocumentElement();                       

            feedback = new Feedback(base, XMLUtils.getChildElement(root, "feedback"));

            features = new Features(root);

            chartControl = new ChartControl(XMLUtils.getChildElement(root, "chart"));

	        statsControl = new StatsControl(XMLUtils.getChildElement(root, "statistics"));

	        databases = new Databases(XMLUtils.getChildElement(root, "databases"));

            Element dataElt = XMLUtils.getChildElement(root, "data");
            quickGO.dataManager.configure(base, dataElt);
            quickGO.monitor.configure(base, XMLUtils.getChildElement(root, "monitor"));
            pages = new Pages(base, root);

            imageArchive.configure(quickGO.uniqueID);

            password = root.getAttribute("password");

            quickGO.updateSchedule.configure(base, dataElt);
        }
        catch (Exception e) {
            failure = e;
            e.printStackTrace();
        }
    }

    public Exception failure;

    public boolean failed() {
        return failure!=null;
    }

    public void close() {
        //if (databaseManager!=null) databaseManager.close();
    }
}
