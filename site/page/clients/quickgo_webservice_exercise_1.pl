#!/usr/bin/perl
# QuickGO web services exercise 1: Find annotations to the GO term 'kidney development' (GO:0001822) for human proteins (taxon = 9606) that use the evidence codes 'IDA' and 'IMP'
use strict;
use warnings;

use LWP::UserAgent;
my $ua = LWP::UserAgent->new;
my $req = HTTP::Request->new(GET => 'http://www.ebi.ac.uk/QuickGO/GAnnotation?goid=GO:0001822&evidence=IDA,IMP&tax=9606&format=gaf&limit=-1');

my $gaf = "gene_association.example";
my $res = $ua->request($req, $gaf);

my $annotation_count = 0;

open (GAF, $gaf);
while (<GAF>) {
	chomp;
	next if /^!/; #ignore comments

	# GAF 2.0 format described at http://www.geneontology.org/GO.format.gaf-2_0.shtml
	my ($db, $db_object_id, $db_object_symbol, $qualifier, $go_id, $reference, $evidence, $with, $aspect, $db_object_name, $db_object_synonym, $db_object_type, $taxon, $date, $assigned_by, $extension, $gene_product_form_id) = split(/\t/);
	print "$db_object_id => $go_id ($aspect) $evidence  $reference  $with  $assigned_by\n";

	$annotation_count++;
}
close GAF;

print "$annotation_count annotations found\n";
exit;
