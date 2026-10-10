# 实验一 大数据系统基本实验 — 代码

SDU 大数据管理与分析 课程实验一配套代码。

## 目录结构

| 文件 | 说明 |
|------|------|
| `HDFSApp.java` | 实验二：HDFS Java API 综合程序（覆盖 10 项 HDFS 操作：上传追加/覆盖、下载自动重命名、cat、文件信息、递归列表、创建/删除文件、创建/删除目录、头部/尾部追加内容、移动文件） |
| `MergeDedup.java` | 实验三·任务1：MapReduce 实现文件合并去重 |
| `SortNum.java` | 实验三·任务2：MapReduce 实现整数排序（输出"位次 数值"） |
| `GrandChild.java` | 实验三·任务3：MapReduce 单表自连接挖掘祖孙关系 |
| `ha` | HDFSApp 运行封装脚本（自动加载 `hadoop classpath`） |
| `data/` | 输入数据样例：`A.txt` `B.txt`（合并去重）、`num1~3.txt`（排序）、`ftable.txt`（祖孙关系） |

## 运行环境

- Ubuntu 22.04 (VirtualBox 虚拟机)
- Hadoop 3.3.6 伪分布式，`fs.defaultFS = hdfs://localhost:9000`
- OpenJDK 1.8

## 编译 / 运行方法

### HDFSApp（Java API）

```bash
cd ~/bigdata/hdfs_lab
javac -cp "$(hadoop classpath)" HDFSApp.java
./ha upload   ~/.bashrc /user/hadoop/test/.bashrc overwrite   # 覆盖上传
./ha download /user/hadoop/test/.bashrc /tmp                  # 下载（同名自动重命名）
./ha cat      /user/hadoop/test/.bashrc                       # 输出到终端
./ha info     /user/hadoop/test/.bashrc                       # 权限/大小/时间/路径
./ha lsr      /user/hadoop                                    # 递归列表
./ha create   /user/hadoop/demo/sub/file1.txt                 # 创建文件（自动建父目录）
./ha delete   /user/hadoop/demo/sub/file1.txt                 # 删除文件
./ha mkdir    /user/hadoop/demo/emptydir
./ha rmdir    /user/hadoop/demo/emptydir                      # 空目录删除 / 非空拒绝
./ha append   /user/hadoop/test/.bashrc "LINE" tail           # 尾部追加（head 为头部追加）
./ha move     /user/hadoop/demo/moved_f.txt /user/hadoop/demo/x.txt
```

### MapReduce

```bash
cd ~/bigdata/mr_lab
javac -cp "$(hadoop classpath)" MergeDedup.java SortNum.java GrandChild.java
jar cf mr.jar *.class

hdfs dfs -mkdir -p /user/sasuke/mr_in
hdfs dfs -put data/* /user/sasuke/mr_in/

hadoop jar mr.jar MergeDedup /user/sasuke/mr_in/A.txt /user/sasuke/mr_in/B.txt /user/sasuke/mr_out/merge
hadoop jar mr.jar SortNum    /user/sasuke/mr_in/num1.txt /user/sasuke/mr_in/num2.txt /user/sasuke/mr_in/num3.txt /user/sasuke/mr_out/sort
hadoop jar mr.jar GrandChild /user/sasuke/mr_in/ftable.txt /user/sasuke/mr_out/gc

hdfs dfs -cat /user/sasuke/mr_out/merge/part-r-00000
hdfs dfs -cat /user/sasuke/mr_out/sort/part-r-00000
hdfs dfs -cat /user/sasuke/mr_out/gc/part-r-00000
```
