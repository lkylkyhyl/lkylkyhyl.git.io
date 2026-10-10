#!/bin/bash
# ha - run HDFSApp with hadoop classpath
export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64
export HADOOP_HOME=/home/sasuke/bigdata/hadoop
cd /home/sasuke/bigdata/hdfs_lab
CP="$($HADOOP_HOME/bin/hadoop classpath)"
exec java -cp ".:$CP" HDFSApp "$@"
