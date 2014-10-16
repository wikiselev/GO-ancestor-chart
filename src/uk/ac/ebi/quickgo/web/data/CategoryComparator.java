package uk.ac.ebi.quickgo.web.data;

import java.util.Comparator;

/**
 * Comparator for Categories
 * 
 * @author cbonill
 * 
 */
public class CategoryComparator implements Comparator {

	@Override
	public int compare(Object o1, Object o2) {
		EnumCategory enumCategory1 = EnumCategory.valueOf(String.valueOf(o1).toUpperCase());
		EnumCategory enumCategory2 = EnumCategory.valueOf(String.valueOf(o2).toUpperCase());

		if (enumCategory1.getOrder() == enumCategory2.getOrder())
			return 0;
		else if (enumCategory1.getOrder() > enumCategory2.getOrder())
			return 1;
		else
			return -1;
	}
}
