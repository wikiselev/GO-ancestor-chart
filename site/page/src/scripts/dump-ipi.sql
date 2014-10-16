select master_id,ipi_id from proteomes.ipi_protein;
write protein,ipi_id postgres+gz ipi_xref.dat.gz;
copy;
