#!/bin/bash
# ==============================================================================
# Hadoop MapReduce Inverted Index 一键编译与作业提交脚本
# ==============================================================================

set -e

# 加载 Hadoop 环境变量
export JAVA_HOME=${JAVA_HOME:-/usr/lib/jvm/java-8-openjdk-amd64}
export HADOOP_HOME=${HADOOP_HOME:-/home/sasuke/bigdata/hadoop}
export PATH=$PATH:$JAVA_HOME/bin:$HADOOP_HOME/bin:$HADOOP_HOME/sbin
export HADOOP_CONF_DIR=$HADOOP_HOME/etc/hadoop

WORK_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$WORK_DIR"

echo "=== 1. 编译 InvertedIndex.java ==="
rm -rf build && mkdir -p build
javac -classpath "$($HADOOP_HOME/bin/hadoop classpath)" -d build src/InvertedIndex.java
echo "编译成功！"

echo "=== 2. 打包生成 InvertedIndex.jar ==="
jar -cvf InvertedIndex.jar -C build .
echo "打包成功：InvertedIndex.jar"

echo "=== 3. 准备 HDFS 输入数据 ==="
hdfs dfs -rm -r -skipTrash /user/sasuke/inverted_index 2>/dev/null || true
hdfs dfs -mkdir -p /user/sasuke/inverted_index/input
hdfs dfs -put input_docs/*.txt /user/sasuke/inverted_index/input/
hdfs dfs -put stopwords.txt /user/sasuke/inverted_index/
echo "输入数据已成功上传至 HDFS！"

echo "=== 4. 提交 MapReduce 作业至 Hadoop YARN 集群 ==="
hadoop jar InvertedIndex.jar InvertedIndex \
    /user/sasuke/inverted_index/input \
    /user/sasuke/inverted_index/output \
    /user/sasuke/inverted_index/stopwords.txt

echo "=== 5. 查看输出结果开头部分 ==="
hdfs dfs -cat /user/sasuke/inverted_index/output/part-r-00000 | head -n 30
echo "=============================================================================="
echo "作业执行成功！完整结果位于 HDFS: /user/sasuke/inverted_index/output/"
echo "=============================================================================="
