-- PUBMED
select pubmed_id,title from quickgo.pubmed where PUBMED_ID in (select ref_db_id from go.protein2go where ref_db_code='PUBMED') order by ''||pubmed_id;
write * postgres+gz PUBMED.dat.gz;
copy;
select pubmed_id,title from quickgo.pubmed order by ''||pubmed_id;
write * postgres+gz PUBMED_FULL.dat.gz;
copy;
-- PROTEIN2GO
select distinct ID , PROTEIN_AC , GO_ID , EVIDENCE , SOURCE , REF_DB_CODE , REF_DB_ID , WITH_DB_CODE , WITH_DB_ID , COPY_FROM , QUALIFIER , EXTRA_TAXID,EXTERNAL_DATE from GO.PROTEIN2GO order by PROTEIN_AC;
write * postgres+gz PROTEIN2GO.dat.gz;
copy;
