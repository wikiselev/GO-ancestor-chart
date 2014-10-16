package uk.ac.ebi.quickgo.web.data;

import java.util.LinkedHashMap;
import java.util.Map;

public class CVExt {
	public static class Item {
	    public String code;
	    public String description;
		public String extra;

	    public Item(String code, String description, String extra) {
	        this.code = code;
	        this.description = description;
		    this.extra = extra;
	    }

		@Override
		public String toString() {
			return "Item{" +
					"code='" + code + '\'' +
					", description='" + description + '\'' +
					", extra='" + extra + '\'' +
					'}';
		}
	}

    public Map<String, Item> vocabulary = new LinkedHashMap<String, Item>();

	public CVExt(Iterable<String[]> from) {
	    for (String[] rs : from) {
	        if (rs[0] != null && rs[0].length() != 0) {
	            vocabulary.put(rs[0], new Item(rs[0], rs[1], rs[2]));
	        }
	    }
	}

	public Item get(String code) {
		return vocabulary.get(code);
	}

	public void dump(String tag) {
		System.out.println("Dump of CV: " + tag);
		for (String key : vocabulary.keySet()) {
			Item item = vocabulary.get(key);
			System.out.println(key + " => " + item);
		}
	}
}
