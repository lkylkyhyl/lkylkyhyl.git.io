"""Generate the seven-page Chinese PDF from verified experiment evidence."""
from pathlib import Path
import json, re
from xml.sax.saxutils import escape
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.lib import colors
from reportlab.lib.styles import ParagraphStyle
from reportlab.lib.enums import TA_CENTER
from reportlab.lib.pagesizes import A4
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle, PageBreak

ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'交付物'; OUT.mkdir(exist_ok=True)
pdfmetrics.registerFont(TTFont('Song',r'C:\Windows\Fonts\simsun.ttc',subfontIndex=0))
pdfmetrics.registerFont(TTFont('Hei',r'C:\Windows\Fonts\simhei.ttf'))
pdfmetrics.registerFont(TTFont('Mono',r'C:\Windows\Fonts\consola.ttf'))
body=ParagraphStyle('body',fontName='Song',fontSize=11.5,leading=18,spaceAfter=8,wordWrap='CJK')
small=ParagraphStyle('small',parent=body,fontSize=9.5,leading=14,spaceAfter=4)
heading=ParagraphStyle('heading',fontName='Hei',fontSize=15,leading=21,spaceBefore=12,spaceAfter=10)
title=ParagraphStyle('title',fontName='Hei',fontSize=21,leading=30,spaceAfter=18,alignment=TA_CENTER)
mono=ParagraphStyle('mono',fontName='Mono',fontSize=8.5,leading=12,spaceAfter=5)
story=[]; md=[]
def p(text,style=body):
    story.append(Paragraph(escape(text).replace('\n','<br/>'),style));md.append(text+'\n')
def h(text):
    p(text,heading);md[-1]='## '+text+'\n'
def table(rows,widths=None):
    data=[[Paragraph(escape(str(c)),small) for c in row] for row in rows]
    t=Table(data,colWidths=widths,repeatRows=1,hAlign='LEFT')
    t.setStyle(TableStyle([('BACKGROUND',(0,0),(-1,0),colors.HexColor('#e9eef3')),('GRID',(0,0),(-1,-1),.5,colors.HexColor('#d9d9d9')),('VALIGN',(0,0),(-1,-1),'MIDDLE'),('LEFTPADDING',(0,0),(-1,-1),8),('RIGHTPADDING',(0,0),(-1,-1),8),('TOPPADDING',(0,0),(-1,-1),5),('BOTTOMPADDING',(0,0),(-1,-1),5)]))
    story.append(t);story.append(Spacer(1,10))
    md.append('| '+' | '.join(map(str,rows[0]))+' |\n')
    md.append('| '+' | '.join(['---']*len(rows[0]))+' |\n')
    md.extend(['| '+' | '.join(map(str,row))+' |\n' for row in rows[1:]])
def page():story.append(PageBreak());md.append('\n---\n')
check=json.loads((ROOT/'results/verification.json').read_text())
assert check['passed'] and check['final_outputs_identical']
hbase=(ROOT/'logs/hbase_java.log').read_text(encoding='utf-8',errors='replace')
shell=(ROOT/'logs/hbase_shell.log').read_text(encoding='utf-8',errors='replace')
assert 'ALL_HBASE_CHECKS_PASSED' in hbase and 'HBASE_SHELL_COMPLETE' in shell
apps=json.loads((ROOT/'results/yarn_apps.json').read_text())['apps']['app']
success=[a for a in apps if a['finalStatus']=='SUCCEEDED' and a['name'].startswith('lab3-pagerank')]
mr=[a for a in success if a['applicationType']=='MAPREDUCE']
spark=[a for a in success if a['applicationType']=='SPARK']
assert len(mr)>=10 and spark

p('实验三 HBase 操作与 PageRank 算法实现',title)
p('计算机科学与技术学院\n大数据管理与分析课程实验报告',ParagraphStyle('sub',parent=body,alignment=TA_CENTER,spaceAfter=20))
table([['项目','信息'],['姓名与学号','侯亚琳  202400390065'],['班级','2024级2班'],['Email','2070575920@qq.com'],['实验日期','2026年11月16日']], [110,365])
h('一 实验目的')
p('通过 HBase Shell 与 Java API 完成表、列族、列及记录的管理，将学生、课程和选课数据映射为宽列表。分别使用 Hadoop MapReduce 与 Spark RDD 实现 PageRank，在 Linux 伪分布式环境中连续迭代 10 次，并比较每轮数值与最终输出。')
h('二 实验软件和硬件环境')
table([['项目','实际环境'],['操作系统','Ubuntu 22.04.5 LTS，VirtualBox 虚拟机'],['资源','内存约 4 GB，单节点，磁盘分区约 51 GB'],['Java 与 Hadoop','OpenJDK 1.8.0_502；Hadoop 3.3.6'],['数据库与计算引擎','HBase 2.5.11；Spark 3.5.3；Python 3'],['部署方式','HDFS + YARN 伪分布式；HBase 单 JVM，数据目录在 HDFS']], [110,365])
p('实验内容包括 HBase 数据导入与表管理，以及 MapReduce、Spark 两种 PageRank 程序的十轮迭代计算。',small)
page()
h('三 HBase 数据模型与原题处理')
p('HBase 使用 RowKey 定位行，以列族、限定符和时间戳定位单元格。列族在建表时定义，限定符可随 Put 动态增加。本实验将基础属性放入 Info，将学生成绩放入 Score；成绩按课程名作为限定符，便于按学生查询全部课程成绩。')
table([['逻辑表','RowKey','列设计'],['Student','S_Name','Info:S_No、S_Name、S_Sex、S_Age；Score:课程名'],['Course','C_No','Info:C_No、C_Name、C_Credit'],['SC','SC_Sno_SC_Cno','Info:SC_Sno、SC_Cno、SC_Score']], [70,115,290])
p('原题中 Mary 与 Lisi 均标记为 2015003，而选课表包含 2015002。为消除引用歧义，将 Mary 学号更正为 2015002，Lisi 保持 2015003。Student 的 RowKey 按题意使用姓名，真实系统中若姓名重复，应改用学号或复合键。')
table([['学号','姓名','性别','年龄'],['2015001','Zhangsan','male','23'],['2015002','Mary','female','22'],['2015003','Lisi','male','24']], [115,160,110,90])
table([['课程号','课程名','学分'],['123001','Math','2.0'],['123002','Computer Science','5.0'],['123003','English','3.0']], [115,270,90])
table([['学生','Math','Computer Science','English'],['Zhangsan','86','null','69'],['Mary','null','77','99'],['Lisi','98','95','null']], [115,90,180,90])
p('SC 保存全部 6 条关系记录，Student 冗余成绩用于演示宽列查询。Java 表名为 lab3_Student、lab3_Course、lab3_SC；Shell 使用 lab3_shell_ 前缀，互不覆盖。初始行数分别为 3、3、6。',small)
page()
h('四 HBase 实现步骤与操作结果')
table([['要求','Java 实现','Shell 对应操作'],['列出表与表信息','listTableDescriptors','list；describe'],['显示指定表所有记录','getScanner(new Scan())','scan'],['添加与删除列族','addColumnFamily / deleteColumnFamily','alter NAME / METHOD => delete'],['添加与删除列','Put / Delete.addColumns','put / deleteall 表,行,列'],['清空表并保留结构','disableTable + truncateTable','truncate'],['统计行数','扫描并累计行数','count']], [112,205,158])
p('createTable 从 fields 提取去重后的列族；若表已存在，先禁用、删除，再创建。addRecord 检查 fields 与 values 长度一致，将 family:qualifier 分解后提交 Put。裸列族约定使用 value 限定符。deleteRow 以整行 Delete 删除记录。')
p('scanColumn 同时支持列族和具体列。程序先扫描现有整行，再按列筛选，使缺失列的现有记录返回 null。例如，查询 Score:Math 时返回 Mary=null。Shell 使用列过滤 scan 展示已有成绩，并用 get 检查 Mary 的 Math 单元格为空。')
p('原题 modifyData(table,row,column) 未提供新值。本实现保留三参数签名，从标准输入读入新值；同时提供四参数重载供脚本调用。将 Zhangsan 的 Math 从 86 改为 90，读取后恢复为 86。')
h('操作结果')
table([['操作','结果'],['统计三张表的行数','Student 为 3，Course 为 3，SC 为 6'],['查询 Mary 的 Math 成绩','返回 null'],['修改 Zhangsan 的 Math 成绩','86 → 90 → 86'],['重建同名操作表','保留新表结构，记录数为 0'],['删除指定列与整行','Extra:y 单元格删除后为空；temp 行删除后记录数为 0'],['清空操作表','表结构保留，记录数为 0']], [170,305])
p('列族增删和清空等操作在 lab3_ops 与 lab3_shell_ops 表上执行，Java 与 Shell 的操作记录保存在 logs 目录。',small)
page()
h('五 PageRank 原理与测试图')
p('将网页表示为有向图的顶点，将超链接表示为有向边。被高排名网页引用，或被多个网页引用的页面通常获得更高排名。阻尼系数 d=0.85，用 0.15 的随机跳转项避免排名完全受闭合链接结构控制。')
p('本实验采用初始 PR0(v)=1 的尺度，排名总量为网页数 N。每轮先求所有无出链页面的排名总量 D，再平均分配给所有 N 个网页。更新公式为：')
p('PR_next(v) = 0.15 + 0.85 × [Σ PR(u)/L(u) + D/N]',ParagraphStyle('formula',parent=body,alignment=TA_CENTER,fontSize=10.5))
p('求和范围为所有指向 v 且出度非零的网页 u，L(u) 为其去重后的出度。将本实验的 PR 值除以 N，即得到总和为 1 的概率尺度。排名总量为 N 时跳转项为 0.15，总量为 1 时跳转项为 0.15/N。')
table([['网页','出链集合','网页','出链集合'],['A','B,C','F','C'],['B','C,D','G','F,H'],['C','A','H','G'],['D','C,E','I','H,J'],['E','D,F','J','空集合']], [65,172,65,173])
p('输入文件 data/graph.tsv 有 10 个节点、15 条边，每行采用“网页 + 制表符 + 英文逗号分隔的出链集合”。J 无出链，其排名在下一轮均匀分配到全图；I 无入链，每轮仍保留跳转项和悬挂贡献。重复边按集合去重，输入中仅作为目标出现的节点也会补齐。')
h('MapReduce 的数据流')
p('状态记录为 page<TAB>rank<TAB>links。Mapper 对本页发出邻接表标记 A，并将 rank/outdegree 贡献发给每个目标页。Reducer 按网页聚合贡献，结合跳转项和悬挂项得到新排名，同时保留邻接表供下一轮读取。')
p('驱动端连续提交 10 个真实 MapReduce 作业，iter00 为初始状态，iter01 至 iter10 为每轮输出。每轮从上一轮状态读取悬挂质量；最终在驱动端按排名排序并格式化为 10 位小数。实验图较小，规范化与最终排序使用驱动内存；大图需要改为分布式预处理、统计和排序。',small)
page()
h('六 Spark 实现与伪分布式运行')
p('Spark 用 textFile 读取 HDFS 图数据，reduceByKey 合并邻接表，通过 distinct 得到顶点全集。links 和 ranks 使用缓存 RDD；每轮先 join 得到页面出链和当前排名，计算悬挂质量，再 flatMap 分发贡献、reduceByKey 汇总。')
p('以顶点全集 leftOuterJoin 贡献，保证没有入链的页面也会得到新的 PR。每轮物化新 ranks，写出状态文件后释放旧排名与 joined 缓存。最后按 (-PR, page) 排序，输出固定 10 位小数。程序恰好迭代 10 次，不使用提前收敛退出。')
h('执行步骤')
for text in [
'1. 启动 NameNode、DataNode、SecondaryNameNode、ResourceManager 和 NodeManager，检查 HDFS 可用。',
'2. 安装并启动 HBase；编译 HBaseLab.java，执行 Java 程序和 Shell 脚本，随后停止 HBase 释放内存。',
'3. 用 hadoop classpath 编译 PageRankMR.java，打包 JAR，将 graph.tsv 上传至 HDFS。',
'4. 提交 PageRankMR 驱动，连续完成 10 个 YARN MapReduce 作业，保留每轮状态和最终结果。',
'5. 通过 spark-submit --master yarn --deploy-mode client 提交 RDD 程序，在单节点 YARN 上连续迭代 10 次。',
'6. 下载 MR 与 Spark 的 final 和 iter01 至 iter10，整理网页排名及各轮 PR 变化。']:p(text)
h('复现命令')
for text in ['bash scripts/setup_linux.sh','bash scripts/start_hadoop.sh','bash scripts/run_hbase.sh','bash scripts/run_mr.sh','bash scripts/run_spark.sh']:p(text,mono)
p('启动脚本假设已有单节点 Hadoop 配置，默认依赖在 ~/bigdata 下，HDFS 地址为 localhost:9000。每次计算使用新的时间戳输出目录，避免覆盖既有结果。源代码、数据、运行脚本和环境说明均随交付包提供。',small)
page()
h('七 十轮计算结果')
final=[]
for line in (ROOT/'results/mapreduce.txt').read_text().splitlines():
    name,value=line[1:-1].split(',');final.append([name,value,value])
table([['网页','MapReduce PR','Spark PR']]+final,[65,205,205])
p('两种最终输出逐行完全一致，均为 (网页,PR值) 格式且保留 10 位小数。内部计算的排名总量保持为 10；表中逐项舍入后的合计为 10.0000000001，差异来自显示精度。J 的悬挂排名已回流到全图，I 在所有轮次均被保留。')
rows=[['轮次','C 的 PR','A 的 PR','J 的 PR']]
for i in range(1,11):
    ranks={p[0]:float(p[1]) for p in (line.split('\t') for line in (ROOT/('results/mr_iter%02d.tsv'%i)).read_text().splitlines())}
    rows.append([i,'%.10f'%ranks['C'],'%.10f'%ranks['A'],'%.10f'%ranks['J']])
table(rows,[55,140,140,140])
p('C 与 A 位于主要链接循环中，PR 值随迭代上升后逐渐趋于平稳。J 的排名由初值 1 降至约 0.2432，其每轮排名均参与下一轮的悬挂质量再分配。各轮数据保存在 results 目录。',small)
page()
h('八 结果分析与体会')
mr=sorted(mr,key=lambda a:a['startedTime'])[-10:]
table([['比较项','MapReduce','Spark'],['迭代组织','每轮提交一个作业，共 10 个作业','同一应用内执行 10 轮 RDD 运算'],['状态保存','每轮将邻接表与 PR 写入 HDFS','图结构与 PR 使用缓存 RDD'],['贡献聚合','Mapper 分发，Reducer 汇总','flatMap 分发，reduceByKey 汇总'],['最终输出','按 PR 降序排列，保留 10 位小数','按 PR 降序排列，保留 10 位小数']], [95,190,190])
p('两种实现得到同样排名。C 与 A 在图的核心循环中，获得的排名高于外围无入链的 I。本实验按要求连续迭代 10 次，并以第 10 轮 PR 值作为最终结果。悬挂节点 J 的排名重新分配后，全图排名总量保持为 10。')
p('MapReduce 每轮将状态写回 HDFS，并启动新的作业；Spark 通过缓存复用图结构和排名，减少重复读取。本实验采用小规模图，运行开销主要来自框架启动和作业调度。为适配 4 GB 虚拟机，HBase 与计算程序顺序运行，并设置计算容器内存。')
h('九 问题与解答')
p('问题一 如何处理无出链网页？\n悬挂节点的排名需要重新分配到全图。本实验每轮统计悬挂质量 D，向每个页面补充 0.85D/N，使排名总量保持 N。')
p('问题二 如何让指定列查询返回缺失值？\nJava 先读取现有整行，再按列族或限定符筛选，将缺少目标列的行映射为 null；Shell 用 get 检查对应的空单元格。')
p('问题三 如何解决 HBase 启动时的 WAL 异常？\n启动日志出现异步 WAL 的 ProtobufDecoder 反射异常。将 hbase.wal.provider 与 hbase.master.store.region.wal.provider 均设为 filesystem 并重启后，元数据初始化完成，Java 与 Shell 操作均成功执行。')
h('参考资料与代码位置')
p('Apache HBase 2.5 Admin API；Apache Spark 3.5.3 RDD Programming Guide；Apache Hadoop 3.3.6 MapReduce Tutorial。官方文档链接及运行方法见 README.md。\n代码仓库：https://github.com/lkylkyhyl/lkylkyhyl.git.io\n实验目录：lab03_pagerank，包含源码、数据、运行脚本、结果与实验报告。',small)

def footer(canvas,doc):
    canvas.setFont('Song',9);canvas.setFillColor(colors.HexColor('#666666'))
    canvas.drawString(60,30,'侯亚琳  202400390065    大数据管理与分析实验三')
    canvas.drawRightString(A4[0]-60,30,str(doc.page))
pdf=OUT/'202400390065-侯亚琳-实验三.pdf'
SimpleDocTemplate(str(pdf),pagesize=A4,rightMargin=60,leftMargin=60,topMargin=44,bottomMargin=48,title='实验三 HBase 操作与 PageRank 算法实现',author='侯亚琳').build(story,onFirstPage=footer,onLaterPages=footer)
(OUT/'实验三报告可编辑稿.md').write_text('\n'.join(md),encoding='utf-8')
print(pdf)
