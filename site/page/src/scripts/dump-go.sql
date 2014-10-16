-- VERSION
select * from (select version,timestamp,url from go.obo_version order by timestamp desc) where rownum<=1;
write * postgres+gz VERSION.dat.gz;
copy;
-- SLIMS
select GO_ID , SUBSET from GO.SLIMS;
write * postgres+gz SLIMS.dat.gz;
copy;
-- XREF & INTERPRO2GO
select GO_ID , DB_CODE , DB_ID from GO.XREFS 
union all 
select go_id,'INTERPRO',entry_ac from go.interpro2go 
union all 
select go_id,'GO',go_id as db_id from go.terms 
union all 
select go_id,'GO',to_char(to_number(substr(go_id,4))) as db_id from go.terms 
union all 
select go_id,'ALT_ID',secondary_id from go.secondaries
union all
select go_id,relation,obsolete_id from go.obsoletes_metadata
order by 1, 2, 3;
write * postgres+gz ALL_XREFS.dat.gz;
copy;
-- CV_CATEGORIES
select CODE , NAME , SORT_ORDER , TERM_NAME from GO.CV_CATEGORIES;
write * postgres+gz CV_CATEGORIES.dat.gz;
copy;
-- CV_DATABASES
select CODE , NAME , SORT_ORDER , ABBREVIATION from GO.CV_DATABASES;
write * postgres+gz CV_DATABASES.dat.gz;
copy;
-- CV_QUALIFIERS
select QUALIFIER , DESCRIPTION from GO.CV_QUALIFIERS;
write * postgres+gz CV_QUALIFIERS.dat.gz;
copy;
-- CV_EVIDENCES
select CODE , NAME , SORT_ORDER from GO.CV_EVIDENCES;
write * postgres+gz CV_EVIDENCES.dat.gz;
copy;
-- CV_RELATIONS
select trim(CODE) as CODE , NAME , SORT_ORDER from GO.CV_RELATIONS;
write * postgres+gz CV_RELATIONS.dat.gz;
copy;
-- TERMS
select GO_ID , NAME , CATEGORY , SLIM , CREATED , UPDATED , IS_OBSOLETE from GO.TERMS order by go_id;
write * postgres+gz TERMS.dat.gz;
copy;
-- CV_SOURCES
select CODE , NAME , SORT_ORDER , ABBREVIATION , GO_REF , GOA_REF from GO.CV_SOURCES;
write * postgres+gz CV_SOURCES.dat.gz;
copy;
-- ANCESTORS
select CHILD_ID , PARENT_ID from GO.ANCESTORS;
write * postgres+gz ANCESTORS.dat.gz;
copy;
-- SYNONYMS
select GO_ID , NAME , TYPE from GO.SYNONYMS order by go_id;
write * postgres+gz SYNONYMS.dat.gz;
copy;
-- XREFS
select GO_ID , DB_CODE , DB_ID from GO.XREFS;
write * postgres+gz XREFS.dat.gz;
copy;
-- TEXT_INDEX_TERM
select ID , FIELD , TEXT from GO.TEXT_INDEX_TERM;
write * postgres+gz TEXT_INDEX_TERM.dat.gz;
copy;
-- RELATIONS
select CHILD_ID , PARENT_ID , trim(RELATION_TYPE) as RELATION_TYPE from GO.RELATIONS;
write * postgres+gz RELATIONS.dat.gz;
copy;
-- CLOGS
select PROTEIN_AC , GO_ID , TRANSLATION_FROM , ORGANISM , ORGANISM_FROM from GO.CLOGS;
write * postgres+gz CLOGS.dat.gz;
copy;
-- DEFINITIONS
select GO_ID , DEFINITION from GO.DEFINITIONS order by go_id;
write * postgres+gz DEFINITIONS.dat.gz;
copy;
-- KEYWORDS2GO
select KEYWORD , GO_ID , SOURCE from GO.KEYWORDS2GO;
write * postgres+gz KEYWORDS2GO.dat.gz;
copy;
-- INTERPRO2GO
select ENTRY_AC , GO_ID , SOURCE from GO.INTERPRO2GO;
write * postgres+gz INTERPRO2GO.dat.gz;
copy;
-- COMMENTS
select GO_ID , COMMENT_TEXT from GO.COMMENTS order by go_id;
write * postgres+gz COMMENTS.dat.gz;
copy;
-- XRF_ABBS
select  ABBREVIATION , DATABASE , GENERIC_URL , URL_SYNTAX from GO.XRF_ABBS
union
select  ABB_SYNONYM , DATABASE , GENERIC_URL , URL_SYNTAX from GO.XRF_ABBS where abb_synonym is not null;
write * postgres+gz XRF_ABBS.dat.gz;
copy;
-- PROTEIN_COMPLEXES
select distinct
  gpa.go_id, gpa.db, gpa.db_object_id, gpi.db_object_symbol, gpi.db_object_name
from
  go.gp_association_inta gpa
join
  go.gp_information_inta gpi on (gpi.db = gpa.db and gpi.db_object_id = gpa.db_object_id)
join
  go.ancestors a on (a.child_id = gpa.go_id)
where
  a.parent_id = 'GO:0043234'
order by
  gpa.go_id
;
write * postgres+gz PROTEIN_COMPLEXES.dat.gz;
copy;
-- ANNOTATION_BLACKLIST
select protein_ac, go_id, reason from go.annotation_blacklist order by protein_ac;
write * postgres+gz ANNOTATION_BLACKLIST.dat.gz;
copy;

