package uk.ac.ebi.quickgo.web.configuration;

import uk.ac.ebi.interpro.common.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TaxonMatcher {
	private static Pattern taxonPattern = Pattern.compile("^(?:taxon:)?([0-9]+)(\\|(?:taxon:)?([0-9]+))?$");
	private static Matcher taxonMatcher = taxonPattern.matcher("");

	public String taxonId;
	public String extraTaxonId;

	public TaxonMatcher(String s) {
		taxonMatcher.reset(s);
		if (taxonMatcher.matches()) {
			taxonId = taxonMatcher.group(1);
			extraTaxonId = StringUtils.nvl(taxonMatcher.group(3));
		}
		else {
			taxonId = "";
			extraTaxonId = "";
		}
	}
}
