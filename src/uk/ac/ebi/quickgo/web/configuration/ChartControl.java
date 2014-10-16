/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package uk.ac.ebi.quickgo.web.configuration;

import java.io.File;
import org.w3c.dom.Element;
import uk.ac.ebi.interpro.common.StringUtils;

/**
 *
 * @author dbinns
 */
public class ChartControl {
    public int defaultImageLimit;
    public int maximumImageLimit;
	public int ancestorLimit;

    ChartControl(Element elt) {
        defaultImageLimit = StringUtils.parseInt(elt != null ? elt.getAttribute("defaultImageLimit") : null, 10);
        maximumImageLimit = StringUtils.parseInt(elt != null ? elt.getAttribute("maximumImageLimit") : null, 20);
	    ancestorLimit = StringUtils.parseInt(elt != null ? elt.getAttribute("ancestorLimit") : null, 1000);
    }
}
