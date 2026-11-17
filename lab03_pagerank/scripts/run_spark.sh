#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64
export HADOOP_HOME=$HOME/bigdata/hadoop
export HADOOP_CONF_DIR=$HADOOP_HOME/etc/hadoop
export SPARK_HOME=$HOME/bigdata/spark
export PATH=$JAVA_HOME/bin:$HADOOP_HOME/bin:$SPARK_HOME/bin:$PATH
export PYSPARK_PYTHON=python3
mkdir -p results logs
RUN_ID=${RUN_ID:-$(date +%Y%m%d%H%M%S)}
ROOT=/user/sasuke/lab3/spark_$RUN_ID
echo "$ROOT" > results/spark_hdfs_path.txt
time spark-submit --master yarn --deploy-mode client --driver-memory 512m --executor-memory 512m --executor-cores 1 --num-executors 1 --conf spark.yarn.am.memory=512m --conf spark.default.parallelism=2 --conf spark.ui.enabled=false src/pagerank_spark.py hdfs:///user/sasuke/lab3/input/graph.tsv "hdfs://$ROOT" > logs/spark.log 2>&1
hdfs dfs -getmerge "$ROOT/final" results/spark.txt
for i in $(seq -w 1 10); do hdfs dfs -getmerge "$ROOT/iter$i" "results/spark_iter$i.tsv"; done
echo SPARK_COMPLETE
