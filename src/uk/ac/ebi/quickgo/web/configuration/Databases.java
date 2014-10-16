package uk.ac.ebi.quickgo.web.configuration;

/**
 * Created by IntelliJ IDEA.
 * User: tonys
 * Date: 11-Aug-2009
 * Time: 14:25:55
 *
 * $Revision: 1.7 $
 *
 * $Log: Databases.java,v $
 * Revision 1.7  2009/08/12 10:10:46  tonys
 * Wasn't correctly handling cases where an attribute was absent
 *
 * Revision 1.6  2009/08/12 08:51:51  tonys
 * Simplified the configuration of databases
 *
 * Revision 1.5  2009/08/12 08:09:10  tonys
 * Made sure that defaultItemURL is ALWAYS initialised to something,,,
 *
 * Revision 1.4  2009/08/12 08:04:04  tonys
 * Trying to track down cause of null pointer exception on the test server
 *
 * Revision 1.3  2009/08/12 07:44:58  tonys
 * Made things a bit more bomb-proof
 *
 * Revision 1.2  2009/08/12 07:03:50  tonys
 * Made default_item_url a configuration parameter
 *
 * Revision 1.1  2009/08/11 15:14:40  tonys
 * Implemented framework to allow URLs in configuration file to override those in XRF_ABBS
 *
 */
import uk.ac.ebi.interpro.common.xml.*;

import java.util.*;

import org.w3c.dom.*;

public class Databases {
	private Map<String, String> genericURLs = new LinkedHashMap<String, String>();
	private Map<String, String> itemURLs = new LinkedHashMap<String, String>();

	private String defaultItemURL = "";

	public Databases(Element databasesRoot) {
		if (databasesRoot != null) {
			defaultItemURL = databasesRoot.getAttribute("default_item_url");

			List<Element> databases = XMLUtils.getChildElements(databasesRoot, "database");
			for (Element db : databases) {
				Attr att = db.getAttributeNode("name");
				if (att != null) {
					String dbName = att.getValue();

					att = db.getAttributeNode("generic_url");
					if (att != null) {
						genericURLs.put(dbName, att.getValue());
					}

					att = db.getAttributeNode("item_url");
					if (att != null) {
						itemURLs.put(dbName, att.getValue());
					}
				}
			}
		}
	}

	public String getGenericURL(String dbName) {
		return genericURLs.get(dbName);
	}

	public String getItemURL(String dbName) {
		String url = itemURLs.get(dbName);
		if (url != null && url.length() == 0) {
			url = defaultItemURL;
		}
		return url;
	}

	public String getDefaultItemURL() {
		return defaultItemURL;
	}
}

