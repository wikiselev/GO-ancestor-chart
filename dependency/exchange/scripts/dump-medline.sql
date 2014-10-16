select EXTERNAL_ID,TITLE from CDB.CITATIONS where source='MED';
write pubmed,title postgres+gz CITATIONS.dat.gz;
copy;
