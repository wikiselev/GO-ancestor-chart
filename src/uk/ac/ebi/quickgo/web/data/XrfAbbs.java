package uk.ac.ebi.quickgo.web.data;

import uk.ac.ebi.quickgo.web.configuration.Configuration;
import uk.ac.ebi.quickgo.web.configuration.DataLocation;
import uk.ac.ebi.quickgo.web.configuration.DataLocation.*;

/**
 * $Revision: 1.7 $
 */
public class XrfAbbs {
	public CV databases;
	public CV genericURLs;
	public CV itemURLs;

	public XrfAbbs(DataLocation directory) throws Exception {
		this.databases = new CV(directory.xrfAbbsInfo.reader(XrfAbbsInfo.ABBREVIATION, XrfAbbsInfo.DATABASE));
		this.genericURLs = new CV(directory.xrfAbbsInfo.reader(XrfAbbsInfo.ABBREVIATION, XrfAbbsInfo.GENERIC_URL));
		this.itemURLs = new CV(directory.xrfAbbsInfo.reader(XrfAbbsInfo.ABBREVIATION, XrfAbbsInfo.URL_SYNTAX));
	}

	public String getDatabase(String source) {
		CV.Item item = databases.get(source);
		return (item != null) ? item.description : "";
	}

	public String getGenericURL(String source) {
		CV.Item item = genericURLs.get(source);
		return (item != null && item.description != null) ? item.description : "";
	}

	public String getGenericURL(Configuration config, String source) {
		// URLs defined in the config file override those defined in XRF_ABBS
		String url = config.databases.getGenericURL(source);
		if (url == null) {
			CV.Item item = genericURLs.get(source);
			url = (item != null && item.description != null) ? item.description : "";
		}

		return url;
	}

	public String getItemURL(Configuration config, String source, String id) {
		// URLs defined in the config file override those defined in XRF_ABBS
		String url = config.databases.getItemURL(source);
		if (url == null) {
			CV.Item item = itemURLs.get(source);
			url = (item != null && item.description != null) ? item.description : config.databases.getDefaultItemURL();
		}

		return url.replaceAll("(?i)\\[example_id\\]", id);
	}
}
