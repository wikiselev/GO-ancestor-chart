package uk.ac.ebi.quickgo.web.configuration;

import org.w3c.dom.Element;
import uk.ac.ebi.interpro.common.StringUtils;

public class StatsControl {
	public int defaultLimit;

	StatsControl(Element elt) {
	    defaultLimit = StringUtils.parseInt((elt != null ? elt.getAttribute("defaultLimit") : null), 80);
	}
}
