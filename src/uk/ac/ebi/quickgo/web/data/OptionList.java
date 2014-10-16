package uk.ac.ebi.quickgo.web.data;

import java.util.ArrayList;

public class OptionList {
	public static class Option {
		public int index;
		public String key;
		public String value;
		public boolean isDefault;

		public Option(String key, String value, boolean isDefault) {
			this.key = key;
			this.value = value;
			this.isDefault = isDefault;
		}
	}

	public ArrayList<Option> options = new ArrayList<Option>();

	public void add(Option option) {
		option.index = options.size();
		options.add(option);
	}

	public void add(String key, String value, boolean isDefault) {
		add(new Option(key, value, isDefault));
	}

	public void add(String key, String value) {
		add(new Option(key, value, false));
	}
}
