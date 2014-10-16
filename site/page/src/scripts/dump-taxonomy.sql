-- ETAXI
select tax_id,parent_id,tree_left,tree_right,sptr_scientific scientific,nvl(sptr_common,ncbi_common) common from TAXONOMY.PUBLIC_NODE order by tax_id;
write * postgres+gz NODE.dat.gz;
copy;
-- TAX_NAME
select tax_id,name_class,'-' as c3,'-' as c4,name,'-' as c6 
from TAXONOMY.TAX_NAME 
join TAXONOMY.TAX_PUBLIC using (tax_id)
order by tax_id;
write * postgres+gz NAME.dat.gz;
copy;
