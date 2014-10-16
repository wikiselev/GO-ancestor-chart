-- GP2PROTEIN
select GENE_ID , PROTEIN_AC , CODE from GO.GP2PROTEIN order by protein_ac;
write * postgres+gz GP2PROTEIN.dat.gz;
copy;
-- GOA_SOURCES
select CODE,NAME,EXTERNAL,SHORT,REF_DB_ID from GO.CV_SOURCES cvs join QUICKGO.GOA_SOURCES gs on (gs.internal=cvs.code);
write * postgres+gz GOA_SOURCES.dat.gz;
copy;
