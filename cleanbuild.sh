#!/usr/local/bin/bash
cd $(dirname $0)

cvs up -dP
./ant.sh clean
./ant.sh get-dependencies
./ant.sh jar
./ant.sh war
./ant.sh zip
rm $(find . -ctime +10 -name 'QuickGO5.1-*.*ar') || echo 'No old jar / war files to delete.'
if [ "$1" = "update" ]; then
	#bsub -q production -M 30000 -R "rusage[mem=30000]" -R "rusage[tmp=80000]" ./update-EBI.sh full
	bsub -q production -J "Update QuickGO PRODUCTION data" -M 38000 -R "rusage[mem=38000]" ./update-EBI.sh full
fi 
