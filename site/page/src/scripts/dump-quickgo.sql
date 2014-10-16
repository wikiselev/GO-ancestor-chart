-- SOURCE2XREF
select CODE,EXTERNAL,GOA_REF,GO_REF from QUICKGO.SOURCE2XREF s;
write * postgres+gz SOURCE2XREF.dat.gz;
copy;
-- DATABASE2XREF
select CODE,EXTERNAL from QUICKGO.DATABASE2XREF d;
write * postgres+gz DATABASE2XREF.dat.gz;
copy;
-- GP2XREF
-- select EXTERNAL from QUICKGO.GP2XREF d;
-- write * postgres+gz GP2XREF.dat.gz;
-- copy;
-- QUICKGO_XREF
select EXTERNAL,NAME from QUICKGO.XREF d;
write * postgres+gz QUICKGO_XREF.dat.gz;
copy;
