#!/bin/bash

if [[ "$JAVA_HOME" == "" ]]; then
	export JAVA_HOME=/sw/arch/pkg/jdk1.5.0_11
fi

${JAVA_HOME}/bin/java -Xms100m -Xmx1000m -jar "$(dirname "$0")"/QuickGO5.1.jar describe "$@"

#end.
