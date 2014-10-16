truncate table quickgo.ipi_xref;
read protein,ipi_id postgres+gz ipi_xref.dat.gz;
insert into quickgo.ipi_xref(protein_ac,id) values (?,?);
copy;
