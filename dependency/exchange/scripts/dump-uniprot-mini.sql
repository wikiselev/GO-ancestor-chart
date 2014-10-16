-- Taxonomy 

select dbe.accession,ts.name_txt,dbe.tax_id
from sptr.dbentry dbe
left outer join  SPTR.NTX_SYNONYM ts on (dbe.tax_id=ts.tax_id and ts.name_class='scientific name')
where
-- SwissProt+TrEMBL
    dbe.entry_type in (0,1) and
-- Non-redundant
    dbe.merge_status!='R' and
-- Not deleted
    dbe.deleted='N' and
-- Not unreleased
    dbe.first_public is not NULL
and accession like 'P9%'
order by accession;
write protein,organism,tax_id postgres+gz sptr-organism.dat.gz;
copy;

-- EC Mapping

select
    d.accession,
    ds.text
from
sptr.dbentry d
left outer join sptr.dbentry_2_description de using(dbentry_id)
left outer join sptr.description_category dc using (dbentry_2_description_id)
left outer join sptr.description_subcategory ds using (category_id)
-- SwissProt+TrEMBL
WHERE d.entry_type in (0,1)
-- Non-redundant
and d.merge_status!='R'
-- Not deleted
and d.deleted='N'
-- 'Public'
and d.first_public is not NULL
-- EC part of description
and SUBCATEGORY_TYPE_ID=3
and accession like 'P9%'
order by d.accession;
write accession,ec tab+gz ec.dat.gz;
copy;

-- Comment topics

select cv.comment_topics_id,cv.topic
from sptr.cv_comment_topics cv;
write topic_id,topic postgres+gz comment_topics.dat.gz;
copy;

-- Comment

select dbe.accession,cb.comment_topics_id,cb.text
from sptr.dbentry dbe 
join sptr.comment_block cb using (dbentry_id)
where
-- SwissProt+TrEMBL
    dbe.entry_type in (0,1) and
-- Non-redundant
    dbe.merge_status!='R' and
-- Not deleted
    dbe.deleted='N' and
-- Not unreleased
    dbe.first_public is not NULL
and accession like 'P9%'
order by dbe.accession;
write protein,topic_id,comment postgres+gz comments.dat.gz; 
copy;


-- Protein information:

select accession,name,gene,db,description from (
select
    d.accession,
    row_number() over (partition by accession order by g.order_in,de.order_in,dc.order_in,ds.order_in) r,
    d.name name,
    gn.name gene,
    case entry_type when 0 then 'S' when 1 then 'T' else '' end db,
    ds.text description
from
sptr.dbentry d
left outer join sptr.gene g using (dbentry_id)
left outer join sptr.gene_name gn using (gene_id)
left outer join sptr.dbentry_2_description de using(dbentry_id)
left outer join sptr.description_category dc using (dbentry_2_description_id)
left outer join sptr.description_subcategory ds using (category_id)
-- SwissProt+TrEMBL
WHERE d.entry_type in (0,1)
-- Non-redundant
and d.merge_status!='R'
-- Not deleted
and d.deleted='N'
-- 'Public'
and d.first_public is not NULL
and accession like 'P9%'
order by d.accession) where r=1;
write protein,name,gene,db,description postgres+gz sptr-description.dat.gz;
copy;

-- Protein gene test (check Q5ZIM6):

select accession,name,gene,db from (
select
    d.accession,
    row_number() over (partition by accession order by g.order_in) r,
    d.name name,
    gn.name gene,
    case entry_type when 0 then 'S' when 1 then 'T' else '' end db
from
sptr.dbentry d
left outer join sptr.gene g using (dbentry_id)
left outer join sptr.gene_name gn using (gene_id)
-- SwissProt+TrEMBL
WHERE d.entry_type in (0,1)
-- Non-redundant
and d.merge_status!='R'
-- Not deleted
and d.deleted='N'
-- 'Public'
and d.first_public is not NULL
and accession like 'P9%'
order by d.accession) where r=1;
write protein,name,gene,db postgres+gz sptr-gene-name.dat.gz;
copy;

-- Secondary accessions

select
    dbe.accession,sa.secondary_accession
from
    sptr.secondary_accession sa,sptr.dbentry dbe
where
    dbe.dbentry_id=sa.dbentry_id and
-- SwissProt+TrEMBL
    dbe.entry_type in (0,1) and
-- Non-redundant
    dbe.merge_status!='R' and
-- Not deleted
    dbe.deleted='N' and
-- Not unreleased
    dbe.first_public is not NULL
and accession like 'P9%'
order by dbe.accession;
write protein,secondary postgres+gz sptr-secondary.dat.gz;
copy;


-- Keywords

select 
	dbe.accession,d2k.keyword_id
from
	sptr.dbentry_2_keyword d2k,sptr.dbentry dbe
where
	d2k.dbentry_id=dbe.dbentry_id and 
	-- SwissProt+TrEMBL
    dbe.entry_type in (0,1) and
-- Non-redundant
    dbe.merge_status!='R' and
-- Not deleted
    dbe.deleted='N' and
-- Not unreleased
    dbe.first_public is not NULL
and accession like 'P9%'
order by dbe.accession;
write protein,keyword postgres+gz sptr-uniprot2keyword.dat.gz;
copy;

-- Keyword CV

select
	keyword_id,keyword
from
	sptr.keyword;
write id,text postgres+gz sptr-keyword.dat.gz;
copy;

-- Cross references

--select
--	accession,database_id,primary_id
--from
--	SPTR.DBENTRY_2_DATABASE d join sptr.dbentry dbe using (dbentry_id)
--where
---- SwissProt+TrEMBL
--    dbe.entry_type in (0,1) and
---- Non-redundant
--    dbe.merge_status!='R' and
---- Not deleted
--    dbe.deleted='N' and
---- Not unreleased
--    dbe.first_public is not NULL
--and accession like 'P9%'
--order by dbe.accession;
--write protein,dbcode,id postgres+gz dbxref.dat.gz;
--copy;

-- Sequence

select accession,sequence
from
    sptr.sequence sq join sptr.dbentry dbe using (dbentry_id)
where
-- SwissProt+TrEMBL
    dbe.entry_type in (0,1) and
-- Non-redundant
    dbe.merge_status!='R' and
-- Not deleted
    dbe.deleted='N' and
-- Not unreleased
    dbe.first_public is not NULL
and accession like 'P9%'
order by accession;
write protein,sequence postgres+gz sequences.dat.gz;
copy;



