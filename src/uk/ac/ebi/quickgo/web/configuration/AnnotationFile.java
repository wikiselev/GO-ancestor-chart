package uk.ac.ebi.quickgo.web.configuration;

abstract public class AnnotationFile extends GPDataFile {
	String virtualGroupingId;

	String db;
	String dbObjectId;
	String qualifier;
	String goId;
	String reference;
	String refDBCode;
	String refDBId;
	String evidence;
	String withString;
	String extraTaxId;
	String date;
	String assignedBy;
	String spliceformId;

	public AnnotationFile(IdMap idMap, DataLocation.NamedFile f, int nCols) throws Exception {
		super(idMap, f, nCols);
	}

	static String[] getReference(String s, String... preferences) {
		if (s == null || s.length() == 0) {
			return null;
		}
		else {
			// the general form of a reference string is:
			//   prefix:suffix[!prefix:suffix]*
			// i.e., a pipe-separated list
			//
			// if there is only one item in the list, then we return that one
			// if there are more than one and the list of preferences is empty, then we return the first one
			// otherwise we return either the first one that matches one of our preferences, or the first one if there is no match
			//
			// preferences is a list of prefixes in ascending order of desirability
			String[] references = s.split("\\|");
			if (references.length == 1) {
				// there is only one item in the list, so return it regardless
				return references[0].split(":", 2);
			}
			else {
				String first = references[0];
				if (preferences == null || preferences.length == 0) {
					// we have no set of preferred references, so just return the first
					return first.split(":", 2);
				}
				else {
					// check each item in the list against our list of preferences
					for (String preference : preferences) {
						for (String reference : references) {
							String[] parts = reference.split(":", 2);
							if (parts[0].equalsIgnoreCase(preference)) {
								return parts;
							}
						}
					}
					// the default position: return the first item in the list
					return first.split(":", 2);
				}
			}
		}
	}

	abstract public int load(AnnotationLoader loader) throws Exception;
}
