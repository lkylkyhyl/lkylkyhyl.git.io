#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64
export HBASE_HOME=$HOME/bigdata/hbase
export HADOOP_HOME=$HOME/bigdata/hadoop
export PATH=$JAVA_HOME/bin:$HBASE_HOME/bin:$HADOOP_HOME/bin:$PATH
mkdir -p logs results build
export HBASE_HEAPSIZE=512
cat > "$HBASE_HOME/conf/hbase-site.xml" <<EOF
<configuration>
<property><name>hbase.rootdir</name><value>hdfs://localhost:9000/user/sasuke/lab3/hbase</value></property>
<property><name>hbase.cluster.distributed</name><value>false</value></property>
<property><name>hbase.zookeeper.property.dataDir</name><value>/home/sasuke/bigdata/lab3/zk</value></property>
<property><name>hbase.unsafe.stream.capability.enforce</name><value>false</value></property>
<property><name>hbase.wal.provider</name><value>filesystem</value></property>
<property><name>hbase.master.store.region.wal.provider</name><value>filesystem</value></property>
</configuration>
EOF
start-hbase.sh
sleep 30
CP=$(hbase classpath)
javac -encoding UTF-8 -source 8 -target 8 -classpath "$CP" -d build src/HBaseLab.java
java -Xmx256m -cp "build:$CP" HBaseLab > logs/hbase_java.log 2>&1
hbase shell -n scripts/hbase_shell.rb > logs/hbase_shell.log 2>&1
stop-hbase.sh
echo HBASE_COMPLETE
