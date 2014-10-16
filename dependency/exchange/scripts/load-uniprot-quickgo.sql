
truncate table quickgo.uniprot;
read protein,name,gene,db postgres+gz sptr-description.dat.gz;
insert into quickgo.uniprot(protein_ac,name,gene,db) values (?,?,?,?);
copy;

--truncate table quickgo.uniprot_xref;
--read protein,dbcode,id postgres+gz dbxref.dat.gz;
--insert into quickgo.uniprot_xref(protein_ac,dbcode,id) values (?,?,?);
--copy;
