package uk.ac.ebi.quickgo.web.configuration;

import uk.ac.ebi.interpro.exchange.RowReader;

public class GPDataFile extends TSVFile {
	private IdMap idMap;
    protected RowReader reader;
    protected String[] columns;

	public GPDataFile(IdMap idMap, DataLocation.NamedFile f, int nCols) throws Exception {
		super(f, nCols);
		this.columns = new String[nCols];
		this.reader = reader();
		this.idMap = idMap;
	}

	public String getVirtualGroupingId(String db, String dbObjectId, boolean mustExist) {
		return idMap.getVirtualGroupingId(db, dbObjectId, mustExist);
	}
}
