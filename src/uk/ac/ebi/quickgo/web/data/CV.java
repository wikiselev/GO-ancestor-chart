package uk.ac.ebi.quickgo.web.data;

import uk.ac.ebi.quickgo.web.configuration.*;
import uk.ac.ebi.interpro.common.database.*;
import uk.ac.ebi.interpro.common.collections.CollectionUtils;
import uk.ac.ebi.interpro.exchange.compress.*;

import java.sql.*;
import java.util.*;
import java.io.*;

public class CV {
	public static class Item {
	    public String code;
	    public String description;

	    public Item(String code, String description) {
	        this.code = code;
	        this.description = description;
	    }

		@Override
		public String toString() {
			return "Item {" +
					"code='" + code + '\'' +
					", description='" + description + '\'' +
					'}';
		}
	}

    public Map<String, Item> vocabulary = new LinkedHashMap<String, Item>();

	private void addItem(String key) {
		if (key != null) {
			vocabulary.put(key, new Item(key, key));
		}
	}

	private void addItem(CV superset, String key) {
		Item value = superset.get(key);
		if (value != null) {
			vocabulary.put(key, value);
		}
	}

    public CV(String... keyList) {
        for (String key : keyList) {
	        addItem(key);
        }
    }

	public CV(ArrayList<String> keys) {
		for (String key : keys) {
			addItem(key);
		}
	}

    public CV(CV superset, String... keyList) {
        for (String key : keyList) {
	        addItem(superset, key);
        }
    }

	public CV(CV superset, ArrayList<String> keys) {
		for (String key : keys) {
			addItem(superset, key);
		}
	}

    public CV(CV superset, Iterable<String[]> map) {
        for (String[] rs : map) {
            String description = superset.get(rs[0]).description;
            vocabulary.put(rs[1], new Item(rs[1], description));
        }
    }

    public void add(String key, String name) {
        vocabulary.put(key, new Item(key, name));
    }

	public void add(String key, Item value) {
	    vocabulary.put(key, value);
	}

	public void add(CV source) {
		vocabulary.putAll(source.vocabulary);
	}

    public CV(Iterable<String[]> from) {
        for (String[] rs : from) {
            if (rs[0] != null && rs[0].length() != 0) {
                vocabulary.put(rs[0], new Item(rs[0], rs[1]));
            }
        }
    }

	public Item get(String key) {
		return vocabulary.get(key);
	}

	public boolean contains(String key) {
		return vocabulary.keySet().contains(key);
	}

	public String[] keys() {
		Set<String> keySet = vocabulary.keySet();
		return keySet.toArray(new String[keySet.size()]);
	}

	public void dump(String tag) {
		System.out.println("Dump of CV: " + tag);
		for (String key : vocabulary.keySet()) {
			Item item = vocabulary.get(key);
			System.out.println(key + " => " + item);
		}
	}
}
