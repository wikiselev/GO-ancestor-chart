#!/bin/bash


curl 'http://www.ebi.ac.uk/QuickGO/GAnnotation?protein=P12345&format=tsv' | tail -n+2 | cut -f 5 | sort -u

#end