#!/bin/bash

# export BIND_IP=$(/sbin/ifconfig eth0 | grep 'inet' | cut -d: -f2 | awk '{ print $2 }')
export BIND_IP="0.0.0.0"
echo $BIND_IP
java -jar $JAR_FILE_PATH $@
