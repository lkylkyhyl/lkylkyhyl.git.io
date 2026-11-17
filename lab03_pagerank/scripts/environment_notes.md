# 运行环境说明

本次使用本机已有 VirtualBox Ubuntu 22.04 虚拟机中的 Hadoop 3.3.6。
Java 为 OpenJDK 8，虚拟机内存约 4 GB。
HDFS 和 YARN 采用单节点伪分布式配置，无需格式化已有 NameNode。

Spark 3.5.3 的 Hadoop 3 发行包和 HBase 2.5.11 由 setup_linux.sh 安装。
安装脚本下载约 690 MB 压缩包，解压后的依赖不收入源码压缩包。

HBase 为 standalone 进程模式，内嵌 ZooKeeper 与 RegionServer，rootdir 指向
HDFS。其部署模式不等同于独立 HMaster、HRegionServer、ZooKeeper 进程集群；
Hadoop 的 HDFS/YARN 与两种 PageRank 作业在真实伪分布式环境运行。

HBase 首次启动出现 AsyncFSWALProvider 的 ProtobufDecoder 反射异常。
按官方排错指南改用 filesystem WAL，并为 Master Region 设置相同 provider。
实际配置快照见 results/hbase-site.xml，原始服务端日志见 logs/hbase_server.log。
HBase 操作完成后停止数据库，再运行 MapReduce 和 Spark，以控制内存占用。

MapReduce 的 AM、Map、Reduce 请求内存均为 512 MB，JVM 最大堆 384 MB；
YARN 按自身最小分配粒度向上取整。Spark driver/executor 堆均为 512 MB，
一个单核 executor；AM 请求 512 MB。请勿仅用配置堆大小估计进程总内存。

源码包不包含 SSH 密钥、密码或虚拟机镜像。迁移到其他机器时，需自行配置
Hadoop 安装路径、HDFS/YARN 连接、用户名路径以及相应依赖。

官方排错说明：https://hbase.apache.org/docs/troubleshooting/
