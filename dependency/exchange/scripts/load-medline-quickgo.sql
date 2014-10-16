truncate table quickgo.pubmed;
read pubmed,title postgres+gz CITATIONS.dat.gz;
insert into quickgo.pubmed(pubmed_id,title) values (?,?);
copy;
