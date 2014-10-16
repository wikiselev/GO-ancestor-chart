package uk.ac.ebi.interpro.exchange.compress;

import java.util.*;

public class Table implements TextList {
    public int search(String value) {
        if (comparator!=null)
            return Arrays.binarySearch(values, value,comparator);
        else
            return Arrays.asList(values).indexOf(value);
    }

    public int size() {
        return values.length;
    }



    public Type type;

    public String[] values;
    private Comparator<String> comparator;

    public Table(String[] values,Type type,Comparator<String> comparator) {
        this.type = type;
        this.values = values;
        this.comparator = comparator;
    }

    public Table(String[] values,Type type) {
        this.type = type;
        this.values = values;
    }

    public String read(int i) {
        return values[i];
    }


    
    public void write(Output<String> output) throws Exception {
        for (String value : values) output.write(value);
        output.close();
    }

	public void dump(String header) {
		System.out.println(header);
		for (int i = 0; i < values.length; i++) {
			System.out.println("[" + i + "]: " + values[i]);
		}
	}

}
