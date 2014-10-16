package uk.ac.ebi.quickgo.web.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import uk.ac.ebi.quickgo.web.servlets.annotation.Summary;
import uk.ac.ebi.quickgo.web.servlets.annotation.Summary.Category;


/**
 * Possible options for downloading statistics. See "stats-options-form" form in "Annotation.xhtml" file
 * @author cbonill
 *
 */
public enum EnumCategory {

	DOWNLOAD_PROTEIN("download_protein", 0),
	DOWNLOAD_GOID("download_goid", 1),
	DOWNLOAD_ASPECT("download_aspect", 2),
	DOWNLOAD_EVIDENCE("download_evidence", 3),
	DOWNLOAD_REFERENCE("download_reference", 4),
	DOWNLOAD_TAXON("download_taxon", 5),
	DOWNLOAD_ASSIGNED("download_assigned", 6),
	DOWNLOAD_BYANNOTATION("download_byannotation", 7),
	DOWNLOAD_BYPROTEIN("download_byprotein", 8),
	DOWNLOAD_BYBOTH("download_byboth", 9);

	String value;
	int order;
	
	EnumCategory(String value, int order) {
		this.value = value;
		this.order = order;
	}

	/**
	 * Get category counts
	 * @param summary Summary report
	 * @param byAnnotation Indicates if the statistics are grouped by annotation
	 * @param byProtein Indicates if the statistics are grouped by protein
	 * @param byBoth Indicates if the statistics are grouped by protein and annotation
	 * @return Statistics counts associated to a category
	 */
	public List<Category> getCategoryCounts(Summary summary, boolean byAnnotation, boolean byProtein, boolean byBoth){				
		
		if (EnumCategory.DOWNLOAD_ASPECT.getValue().equals(this.value)) {
			return addValues(summary.aspectByAnnotation, summary.aspectByProtein, byAnnotation, byProtein, byBoth);
		} else if (EnumCategory.DOWNLOAD_ASSIGNED.getValue().equals(this.value)) {
			return addValues(summary.sourceByAnnotation, summary.sourceByProtein, byAnnotation, byProtein, byBoth);
		} else if (EnumCategory.DOWNLOAD_EVIDENCE.getValue().equals(this.value)) {			
			return addValues(summary.evidenceByAnnotation, summary.evidenceByProtein, byAnnotation, byProtein, byBoth);				 
		} else if (EnumCategory.DOWNLOAD_GOID.getValue().equals(this.value)) {
			return addValues(summary.termByAnnotation, summary.termByProtein, byAnnotation, byProtein, byBoth);
		} else if (EnumCategory.DOWNLOAD_PROTEIN.getValue().equals(this.value)) {
			return Arrays.asList(summary.proteinStatistics);
		} else if (EnumCategory.DOWNLOAD_REFERENCE.getValue().equals(this.value)) {
			return addValues(summary.referenceByAnnotation, summary.referenceByProtein, byAnnotation, byProtein, byBoth);
		} else if (EnumCategory.DOWNLOAD_TAXON.getValue().equals(this.value)) {
			return addValues(summary.taxonByAnnotation, summary.taxonByProtein, byAnnotation, byProtein, byBoth);
		}
		return new ArrayList<Category>();
	}
	
	/**
	 * Given a category and the boolean count values, returns a list with the different category counts
	 * @param byAnnotationCategory Annotation count of the category
	 * @param byProteinCategory Protein count of the category
	 * @param byAnnotation Boolean for Annotation
	 * @param byProtein Boolean for Protein
	 * @param byBoth Boolean for Protein and annotation
	 * @return List with counts
	 */
	public static List<Category> addValues(Category byAnnotationCategory, Category byProteinCategory, boolean byAnnotation, boolean byProtein, boolean byBoth){
		List<Category> categoryCounts = new ArrayList<Category>();
		if (byBoth) {
			categoryCounts.add(byAnnotationCategory);
			categoryCounts.add(byProteinCategory);
		} else if (byProtein) {
			categoryCounts.add(byProteinCategory);
		} else if (byAnnotation) {
			categoryCounts.add(byAnnotationCategory);
		}
		return categoryCounts;
	}
	
	/**
	 * To check if a category is a "by" one
	 * @return True if it is a "by" category. False otherwise
	 */
	public boolean isByCategory(){
		return EnumCategory.DOWNLOAD_BYANNOTATION.getValue().equals(this.value) || EnumCategory.DOWNLOAD_BYPROTEIN.getValue().equals(this.value) || EnumCategory.DOWNLOAD_BYBOTH.getValue().equals(this.value);
	}
	
	/**
	 * To get all the categories values
	 * @return Set with all the categories values
	 */
	public static Set<String> getAsSet(){
		Set<String> enumValues = new TreeSet<String>();
		for(EnumCategory enumCategory : values()){
			enumValues.add(enumCategory.getValue());
		}
		return enumValues;
	}
	
	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}
		
	public int getOrder() {
		return order;
	}

	public void setOrder(int order) {
		this.order = order;
	}
}