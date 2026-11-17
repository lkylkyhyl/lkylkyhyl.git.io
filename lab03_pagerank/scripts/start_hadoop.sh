#!/usr/bin/env bash
set -euo pipefail
export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64
export HADOOP_HOME=$HOME/bigdata/hadoop
export PATH=$JAVA_HOME/bin:$HADOOP_HOME/bin:$HADOOP_HOME/sbin:$PATH
for daemon in namenode datanode secondarynamenode; do hdfs --daemon start "$daemon" || true; done
for daemon in resourcemanager nodemanager; do yarn --daemon start "$daemon" || true; done
sleep 8
jps
hdfs dfsadmin -report
