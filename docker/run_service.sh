#!/bin/bash

# export BIND_IP=$(/sbin/ifconfig eth0 | grep 'inet' | cut -d: -f2 | awk '{ print $2 }')
export BIND_IP="0.0.0.0"
echo "BIND IP: $BIND_IP"
export CLUSTER_IP=$(curl -s http://whatismyip.akamai.com/)
echo "CLUSTER IP: $CLUSTER_IP"

java -jar $JAR_FILE_PATH $@ > ras.log
