select protein_ac,dbcode,method_ac,pos_from,pos_to,status from interpro.match order by protein_ac,method_ac,pos_from;
write * tab match.dat;
copy;
