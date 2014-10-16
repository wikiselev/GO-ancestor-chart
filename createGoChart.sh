#!/bin/bash

RESOLVE_LINK=`dirname $0`
CURRENT_DIR=`pwd`
QUICK_GO_HOME=${RESOLVE_LINK}
LIB_PATH=${QUICK_GO_HOME}/lib

#echo ${LIB_PATH}

GO_ID_FILE=$1
OUTPUT_PREFIX=goGraph

CLASSPATH="${QUICK_GO_HOME}/QuickGO5.1-BI.jar:${LIB_PATH}/batik-1.7/batik.jar:${LIB_PATH}/batik-1.7/lib/batik-svggen.jar"

if [ $# -lt 1 ] || [ ! -f ${GO_ID_FILE} ]; then
     echo "Usage: "
     echo "       $0 fileName"     
     echo "              will generate a graph using the GO ids contained in the given file."
     echo ""
     echo "              The file can contain one GO id per line. You can also include html color"
     echo "              code, all GO id following will have this color."
     echo ""
     exit 1
fi

#echo $CLASSPATH

java -Xmx528M -cp ${CLASSPATH}  uk.ac.ebi.quickgo.web.graphics.HierarchyGraph --terms=${GO_ID_FILE} --name=${OUTPUT_PREFIX} --base=${QUICK_GO_HOME}/data/mini


