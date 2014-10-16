#!/bin/bash


cd $(dirname $0)

which=$(ls -1tr QuickGO5.1-*.war | tail -n1)

echo "Deploying $which"

ln -sf $which QuickGO5.1.war


if [[ "$4" == "" ]]; then echo "Insufficient Parameters";exit 1; fi

pass="$1"
host="$2"
path="$3"
config="$4" 
	
	

echo "Deploying $config to $host$path"

curl -u "tomcat:$pass" "http://$host/manager/undeploy?path=$path"

curl -u "tomcat:$pass" "http://$host/manager/deploy?path=$path&config=file:$config"


#end
