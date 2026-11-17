# 实验三 HBase 与 PageRank

作者：侯亚琳，202400390065，2024级2班，2070575920@qq.com。

## 内容

- `src/HBaseLab.java`：HBase Java API，表管理与数据增删改查。
- `scripts/hbase_shell.rb`：同等 Shell 操作、三表数据导入、增删改查。
- `src/PageRankMR.java`：真实 Hadoop MapReduce，连续 10 个作业。
- `src/pagerank_spark.py`：Spark RDD，YARN client 模式，连续 10 轮。
- `data/graph.tsv`：10 个节点、15 条边，J 无出链，I 无入链。
- `results/`：两个框架的最终排名、逐轮状态与辅助计算结果。
- `logs/`：HBase 操作记录、服务启动日志与计算作业运行日志。
- `交付物/`：PDF 实验报告和完整压缩包。

## 环境与运行

已在 Ubuntu 22.04、Java 8、Hadoop 3.3.6 下运行。Spark 3.5.3，HBase 2.5.11。
默认依赖位于 `$HOME/bigdata/{hadoop,hbase,spark}`，项目放在任意可写目录。
HDFS 默认地址 `hdfs://localhost:9000`，用户名路径为 `/user/sasuke/lab3`。
如在不同机器复现，修改脚本中的安装位置、用户路径与 HDFS 地址。

```bash
bash scripts/setup_linux.sh  # 下载并解压缺失的 HBase 与 Spark
bash scripts/start_hadoop.sh
bash scripts/run_hbase.sh
bash scripts/run_mr.sh
bash scripts/run_spark.sh
python3 scripts/verify_results.py
```

必须先配置好单节点 HDFS/YARN；脚本不会格式化 NameNode。
HBase 实验采用单 JVM 内嵌 ZooKeeper/RegionServer，数据存于 HDFS。
HBase 结束后再运行计算作业，适配 4 GB 虚拟机。
当前环境的异步 WAL 存在依赖兼容问题，run_hbase.sh 已将普通 WAL 和
Master Region WAL provider 设为 filesystem，以避免 ProtobufDecoder 反射异常。
每次 PageRank 运行使用时间戳输出目录，已有输出不覆盖。
若同一秒内重复执行，可设置 `RUN_ID=自定义唯一值`。

## 算法与格式

输入为 `网页<TAB>目标1,目标2`，空目标表示悬挂页。
重复源行合并出链，重复边去重，仅作为目标出现的节点也计入全集。
PR 初始为 1，阻尼系数 0.85，总量为节点数 N。

`PR_next(v) = 0.15 + 0.85 * (sum(PR(u)/outdegree(u)) + dangling_mass/N)`

恰好迭代 10 次，不以收敛提前退出。内部使用 double，最后输出
`(网页,PR值)`，固定 10 位小数，按 PR 降序、同分按名称升序。
PR/N 可转换为总和为 1 的概率形式。

MapReduce 的图规范化、每轮悬挂质量读取以及最终排序在驱动端完成，
适用于实验小图；每轮贡献分发和聚合均在真正的 Map/Reduce task 中完成。
大图应将规范化、悬挂质量聚合与全局排序改成分布式阶段。
Spark 图和排名保存在缓存 RDD，所有顶点用 leftOuterJoin 保留。

## 原题歧义及处理

1. Student 原表 Mary 与 Lisi 均为 2015003，但 SC 使用 2015002。
   将 Mary 更正为 2015002，保留原姓名、性别、年龄，保证引用可解释。
2. Student RowKey 按题目使用姓名，姓名非唯一时需扩展为学号或复合键；
   Course RowKey 为课程号，SC 为 `学号_课程号`；Student Score 冗余成绩。
3. `modifyData(table,row,column)` 没有新值参数：保留该签名从标准输入读取，
   并提供四参数重载供可复现实验调用。
4. fields 为裸列族时，addRecord 约定限定符为 `value`。
5. scanColumn 扫描整行再筛选，确保缺失列的现有行返回 null；
   原生 Shell 的列过滤 scan 会省略该行，可通过 get 检查空结果。
6. createTable 按题意删除并重建同名表；演示仅操作独立 `lab3_` 前缀表。
   请勿对真实业务表调用这些破坏性接口。

## 提交

代码与实验资料：https://github.com/lkylkyhyl/lkylkyhyl.git.io/tree/main/lab03_pagerank 。

官方资料：
- https://hbase.apache.org/2.5/apidocs/org/apache/hadoop/hbase/client/Admin.html
- https://spark.apache.org/docs/3.5.3/rdd-programming-guide.html
- https://hadoop.apache.org/docs/r3.3.6/hadoop-mapreduce-client/hadoop-mapreduce-client-core/MapReduceTutorial.html
