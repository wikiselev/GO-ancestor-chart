package uk.ac.ebi.quickgo.web.graphics;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFCell;
import org.apache.poi.xssf.streaming.SXSSFRow;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import uk.ac.ebi.quickgo.web.data.CategoryComparator;
import uk.ac.ebi.quickgo.web.data.EnumCategory;
import uk.ac.ebi.quickgo.web.servlets.annotation.Summary;
import uk.ac.ebi.quickgo.web.servlets.annotation.Summary.Bucket;
import uk.ac.ebi.quickgo.web.servlets.annotation.Summary.Category;

/**
 * Class to generate Excel file for the statistics
 * 
 * @author cbonill
 * 
 */
public class ExcelExporter {

	SXSSFWorkbook workBook = new SXSSFWorkbook();

	/**
	 * Generate a statistics file depending on the parameters
	 * 
	 * @param summary
	 *            Summary report
	 * @param parameters
	 *            Parameters to be exported
	 * @throws IOException
	 */
	public ByteArrayOutputStream generateFile(Summary summary,
			List<String> parameters) throws IOException {
		
		// create a new file
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		
		// Sort the options like they appear in the pop-up
		Collections.sort(parameters, new CategoryComparator());
		
		// Create the sheets. The number of sheets will depend on the parameters
		createSheets(parameters, workBook);

		// Write values
		writeSheets(summary, parameters);

		// write the workbook to the output stream
		// close our file (don't blow out our file handles
		workBook.write(out);
		out.close();
		return out;
	}

	/**
	 * Create sheets depending on the parameters
	 * 
	 * @param categories
	 *            List of parameters obtained from the form
	 * @param workbook
	 *            Workbook to create
	 */
	private void createSheets(List<String> categories, Workbook workbook) {
		int index = 0;

		// Create "summary" sheet
		workbook.createSheet();
		workbook.setSheetName(index, "summary");
		index++;

		// Create rest of the sheets
		for (String category : categories) {
			EnumCategory enumCategory = EnumCategory.valueOf(category.toUpperCase());// Get the corresponding Enum object

			// Omit the "by" parameter
			if (!enumCategory.isByCategory()) {
				// Create a sheet for each category
				workbook.createSheet();
				workbook.setSheetName(index, category.replace("download_", ""));// Remove the
																				// "download_" string
																				// from the name
				index++;
			}
		}
	}

	/**
	 * Write values is sheets
	 * 
	 * @param summary
	 *            Summary
	 * @param categories
	 *            Categories to write
	 */
	private void writeSheets(Summary summary, List<String> categories) {

		// Get the "grouped by" parameter
		boolean byProtein = categories.contains(EnumCategory.DOWNLOAD_BYPROTEIN	.getValue());
		boolean byAnnotation = categories.contains(EnumCategory.DOWNLOAD_BYANNOTATION.getValue());
		boolean byBoth = categories.contains(EnumCategory.DOWNLOAD_BYBOTH.getValue());

		// Write the summary sheet
		Sheet sheet = workBook.getSheet("summary");// Get the "summary" sheet
		writeSummarySheet(sheet, summary.totalAnnotations, summary.totalProteins);

		// Write the rest of the sheets
		for (String category : categories) {
			EnumCategory enumCategory = EnumCategory.valueOf(category.toUpperCase());// Get the corresponding Enum object

			if (!enumCategory.isByCategory()) {
				sheet = workBook.getSheet(category.replace("download_", ""));// Get category sheet
				List<Category> counts = enumCategory.getCategoryCounts(summary,	byAnnotation, byProtein, byBoth);// Calculate the category counts
				writeSheet(sheet, counts);// Write the values
			}
		}
	}

	/**
	 * Write the "summary" sheet
	 * 
	 * @param sheet
	 *            Summary sheet
	 * @param totalAnnotations
	 *            Number of total annotations to write
	 * @param totalProteins
	 *            Number of total proteins to write
	 */
	private void writeSummarySheet(Sheet sheet, int totalAnnotations,
			int totalProteins) {
		
		CellStyle cellStyle = boldFontStyle();
		// Title
		SXSSFRow r = (SXSSFRow) sheet.createRow(1);// Row 1
		r.setRowStyle(cellStyle);
		SXSSFCell SXSSFCell = (SXSSFCell) r.createCell(0);
		SXSSFCell.setCellValue("Summary");
		// Values
		r = (SXSSFRow) sheet.createRow(2);// Row 2
		SXSSFCell = (SXSSFCell) r.createCell(0);
		SXSSFCell.setCellValue("Number of annotations: " + totalAnnotations);
		r = (SXSSFRow) sheet.createRow(3);// Row 3
		SXSSFCell = (SXSSFCell) r.createCell(0);
		SXSSFCell.setCellValue("Number of distinct proteins: " + totalProteins);

	}

	/**
	 * Write values in a specific sheet
	 * 
	 * @param sheet
	 *            Sheet to be written
	 * @param counts
	 *            Values to write
	 */
	private void writeSheet(Sheet sheet, List<Category> counts) {

		// SXSSFCell style
		CellStyle cellStyle;
		// SXSSFRow
		SXSSFRow r = null;
		// SXSSFCell
		SXSSFCell c = null;
		// Column to start writing the values of the second count (if any)
		int offset = 10;

		// Lists of buckets for each category count
		List<Bucket> firstSetOfBuckets = new ArrayList<Bucket>();
		List<Bucket> secondSetOfBuckets = new ArrayList<Bucket>();

		// Category counts title
		String firstTitle = null, secondTitle = null;

		if (counts.size() >= 1) {

			// First
			firstTitle = getTitle(counts.get(0));
			firstSetOfBuckets = getBuckets(counts.get(0));

			// Get the buckets and title for each one

			// If there is just one, set the offset to 0 to start writing values in the first column
			if (counts.size() == 1) {
				offset = 0;
			} else if (counts.size() == 2) {
				secondSetOfBuckets = getBuckets(counts.get(1));
				secondTitle = getTitle(counts.get(1));
			}

			Iterator<Bucket> firstBuckIterator = firstSetOfBuckets.iterator();
			Iterator<Bucket> secondBuckIterator = secondSetOfBuckets.iterator();

			int rownum = 1;

			cellStyle = boldFontStyle();

			// Set titles
			r = (SXSSFRow) sheet.getRow(rownum);
			if (r == null) {
				r = (SXSSFRow) sheet.createRow(rownum);
			}
			setTitle(0, r, c, cellStyle, firstTitle);
			setTitle(offset, r, c, cellStyle, secondTitle);

			rownum++;

			// Set column headers
			setHeaders(sheet, r, c, 0, rownum, cellStyle);
			setHeaders(sheet, r, c, offset, rownum, cellStyle);
			rownum++;
			
			// Write values
			writeValues(firstBuckIterator, secondBuckIterator, r, c, rownum, sheet, offset);		
		}
	}

	/**
	 * Write statistics values
	 * @param firstBuckIterator First set of values to write
	 * @param secondBuckIterator Second set of values to write (if any) 
	 * @param r Row
	 * @param c Column
	 * @param rownum Row number
	 * @param sheet Sheet
	 * @param offset Offset
	 */
	private void writeValues(Iterator<Bucket> firstBuckIterator, Iterator<Bucket> secondBuckIterator, SXSSFRow r, SXSSFCell c, int rownum, Sheet sheet, int offset){
		// Write values while any of the categories counts has buckets
		while (firstBuckIterator.hasNext() || secondBuckIterator.hasNext()) {

			// Create row
			r = (SXSSFRow) sheet.getRow(rownum);
			if (r == null) {
				r = (SXSSFRow) sheet.createRow(rownum);
			}

			// Write bucket values
			if (firstBuckIterator.hasNext()) {
				writeBucketValues(sheet, r, c, 0, rownum,
						firstBuckIterator.next());
			}
			if (secondBuckIterator.hasNext()) {
				writeBucketValues(sheet, r, c, offset, rownum,
						secondBuckIterator.next());
			}
			rownum++;
		}
	}

	/**
	 * Creates a style with font bold
	 * @return Bold font style
	 */
	private CellStyle boldFontStyle() {
		CellStyle cellStyle = workBook.createCellStyle();
		// Create bold font for the titles and column headers
		Font font = workBook.createFont();
		font.setBoldweight(Font.BOLDWEIGHT_BOLD);
		cellStyle.setFont(font);

		return cellStyle;
	}

	/**
	 * Set the title (by annotation or by protein)
	 * 
	 * @param index
	 * @param r
	 *            Row
	 * @param c
	 *            Column
	 * @param cellStyle
	 *            Cell style
	 * @param title
	 *            Title to write
	 */
	private void setTitle(int index, SXSSFRow r, SXSSFCell c,
			CellStyle cellStyle, String title) {
		if (title != null && title.trim().length() > 0) {
			c = (SXSSFCell) r.createCell(index);
			c.setCellStyle(cellStyle);
			c.setCellValue(title);
		}
	}

	/**
	 * Write header values
	 * 
	 * @param sheet
	 *            Sheet
	 * @param r
	 *            SXSSFRow
	 * @param c
	 *            Column
	 * @param rownum
	 *            SXSSFRow number
	 * @param cellStyle
	 *            SXSSFCell style
	 */
	private void setHeaders(Sheet sheet, SXSSFRow r, SXSSFCell c, int offset,	int rownum, CellStyle cellStyle) {
		r = (SXSSFRow) sheet.getRow(rownum);
		if (r == null) {
			r = (SXSSFRow) sheet.createRow(rownum);
		}
		r.setRowStyle(cellStyle);
		c = (SXSSFCell) r.createCell(offset);// First column
		c.setCellValue("Code");
		c = (SXSSFCell) r.createCell(offset + 1);// Second columns
		c.setCellValue("Name");
		c = (SXSSFCell) r.createCell(offset + 2);// Third column
		c.setCellValue("Percentage");
		c = (SXSSFCell) r.createCell(offset + 3);// Third column
		c.setCellValue("Count");
	}

	/**
	 * Write bucket values
	 * 
	 * @param sheet
	 *            Sheet
	 * @param r
	 *            Row
	 * @param c
	 *            Column
	 * @param rownum
	 *            Row number
	 * @param bucket
	 *            Bucket to write
	 */
	private void writeBucketValues(Sheet sheet, SXSSFRow r, SXSSFCell c, int offset, int rownum, Bucket bucket) {
		if (bucket != null) {
			c = (SXSSFCell) r.createCell(offset);// First column
			c.setCellValue(bucket.code);
			c = (SXSSFCell) r.createCell(offset + 1);// Second columns
			c.setCellValue(bucket.name);
			c = (SXSSFCell) r.createCell(offset + 2);// Third column
			c.setCellValue(bucket.percentage());
			c = (SXSSFCell) r.createCell(offset + 3);// Fourth column
			c.setCellValue(bucket.count);
		}
	}
	
	/**
	 * Get name from a category
	 * @param category Category
	 * @return Category name
	 */
	private String getTitle(Category category) {
		return category.name;
	}

	/**
	 * Get category buckets
	 * @param category Category
	 * @return Category buckets
	 */
	private List<Bucket> getBuckets(Category category) {
		return category.buckets;
	}
}