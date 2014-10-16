truncate table priam;
read protein_ac,priam_id,pos_from,pos_to,profile_from,profile_to,extend_from,extend_to,pct_profile,pct_identity,e_value,bit_score tab priam.dat;
replace protein_ac,priam_id,pos_from,pos_to,profile_from,profile_to,extend_from,extend_to,pct_profile,pct_identity,e_value,bit_score priam;
copy;
