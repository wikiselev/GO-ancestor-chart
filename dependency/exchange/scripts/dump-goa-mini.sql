-- PROTEIN2GO
select distinct ID , PROTEIN_AC , GO_ID , EVIDENCE , SOURCE , REF_DB_CODE , REF_DB_ID , WITH_DB_CODE , WITH_DB_ID , COPY_FROM , QUALIFIER , EXTRA_TAXID,EXTERNAL_DATE from GO.PROTEIN2GO where protein_ac like 'P9%' order by PROTEIN_AC;
write * postgres+gz PROTEIN2GO.dat.gz;
copy;
