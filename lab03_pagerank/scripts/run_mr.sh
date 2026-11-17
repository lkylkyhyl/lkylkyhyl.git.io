#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64
export HADOOP_HOME=$HOME/bigdata/hadoop
export HADOOP_CONF_DIR=$HADOOP_HOME/etc/hadoop
export PATH=$JAVA_HOME/bin:$HADOOP_HOME/bin:$HADOOP_HOME/sbin:$PATH
mkdir -p build results logs
javac -encoding UTF-8 -source 8 -target 8 -classpath "$(hadoop classpath)" -d build src/PageRankMR.java
jar cf build/pagerank.jar -C build .
hdfs dfs -mkdir -p /user/sasuke/lab3/input
hdfs dfs -put -f data/graph.tsv /user/sasuke/lab3/input/graph.tsv
RUN_ID=${RUN_ID:-$(date +%Y%m%d%H%M%S)}
ROOT=/user/sasuke/lab3/mr_$RUN_ID
echo "$ROOT" > results/mr_hdfs_path.txt
time hadoop jar build/pagerank.jar PageRankMR /user/sasuke/lab3/input/graph.tsv "$ROOT" > logs/mapreduce.log 2>&1
hdfs dfs -getmerge "$ROOT/final" results/mapreduce.txt
for i in $(seq -w 1 10); do hdfs dfs -getmerge "$ROOT/iter$i" "results/mr_iter$i.tsv"; done
echo MAPREDUCE_COMPLETE
