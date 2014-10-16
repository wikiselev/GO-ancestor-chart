#!/bin/bash

cd $(dirname $0)

which="$1"
action="$2"
version="$3"
if [[ "${version}" == "" ]]; then version="new"; fi
if [[ "${action}" == "" ]]; then action="update"; fi

templocation=tmp/${which}

#mkdir -p $(readlink -m $templocation)
mkdir -p ${templocation}

if [[ "${action}" == "update"  || "${action}" == "download" ]]; then
	rm -r ${templocation}/* || echo 'Nothing to delete'
	#echo 'Archive Delete disabled'
#	for x in data/${which}/*; do if [[ -d "$x" ]]; then rm -rf "$x"; fi;done
	rm $(find data/${which} -ctime +10 -name '*20*.zip') || echo 'Nothing to delete'
fi

echo "Java: ${java} ${args}"

"${java}" ${args} -jar QuickGO5.1.jar ${action} ${templocation} ${version} origin/${which} data/${which}

if [[ "${action}" == "update"  || "${action}" == "archive" || "${action}" == "link" ]]; then
	( 
		rm -r ${templocation}/*
		echo 'Done'
		cd data/${which}
		ln -sf $(ls -1t 20*.zip | head -n1) latest.zip
		ln -sf $(ls -1t QuickGO5.1-20*.zip | head -n1) QuickGO5.1-latest.zip

		ftp_dir=/ebi/ftp/pub/contrib/goa/QuickGO/${which}
		# blow away data files that are more than a week old
		rm $(find ${ftp_dir} -ctime +7 -name '*20*.zip') || echo 'Nothing to delete'
		# copy the latest stamp and data files to the ftp location from which QuickGO will retrieve them
		cp $(ls -1t quickgo-stamp-v*.txt | head -n1) ${ftp_dir}
		cp $(ls -1t 20*.zip | head -n1) ${ftp_dir}
	)
fi

echo "QuickGO update calculated"
#end
