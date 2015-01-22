# GO-ancestor-chart
Java package to build and plot GO ancestor chart (homo sapiens only) based on a set of GO terms

Once you unpack the tar.gz, the command to run is:

java -cp "./lib/*.jar:./QuickGO5.1-BI.jar:./lib/batik-1.7/batik.jar"  uk.ac.ebi.quickgo.web.graphics.HierarchyGraph --terms=goTerms --name=goGraph


"goTerms" is the name/path of a file containing one GO id per line.

"goGraph" is the prefix used for the output files.
