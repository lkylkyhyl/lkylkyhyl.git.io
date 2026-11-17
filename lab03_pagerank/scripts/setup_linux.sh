#!/usr/bin/env bash
set -euo pipefail
export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64
export HADOOP_HOME=$HOME/bigdata/hadoop
export PATH=$JAVA_HOME/bin:$HADOOP_HOME/bin:$HADOOP_HOME/sbin:$PATH
export HADOOP_CONF_DIR=$HADOOP_HOME/etc/hadoop
mkdir -p "$HOME/bigdata" logs
cd "$HOME/bigdata"
if [ ! -d hbase ]; then
  curl -fL --retry 2 https://repo.huaweicloud.com/apache/hbase/2.5.11/hbase-2.5.11-bin.tar.gz -o hbase.tgz
  tar xzf hbase.tgz
  ln -s hbase-2.5.11 hbase
fi
if [ ! -d spark ]; then
  curl -fL --retry 2 https://repo.huaweicloud.com/apache/spark/spark-3.5.3/spark-3.5.3-bin-hadoop3.tgz -o spark.tgz
  tar xzf spark.tgz
  ln -s spark-3.5.3-bin-hadoop3 spark
fi
echo 'DEPENDENCIES_READY'
