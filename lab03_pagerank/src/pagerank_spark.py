"""RDD PageRank, initial rank=1, damping=0.85, exactly 10 iterations."""
import sys
from operator import add
from pyspark import SparkContext

def parse(line):
    fields = line.split('\t')
    if len(fields) != 2 or not fields[0].strip():
        raise ValueError('Expected page TAB comma-separated links: ' + repr(line))
    return fields[0].strip(), tuple(sorted(set(x.strip() for x in fields[1].split(',') if x.strip())))

def main():
    if len(sys.argv) != 3:
        raise SystemExit('spark-submit pagerank_spark.py input outputRoot')
    sc = SparkContext(appName='lab3-pagerank-spark')
    sc.setLogLevel('WARN')
    records = sc.textFile(sys.argv[1]).filter(lambda s: bool(s.strip())).map(parse)
    adjacency = records.reduceByKey(lambda a,b: tuple(sorted(set(a+b))))
    vertices = records.flatMap(lambda p: [p[0]] + list(p[1])).distinct().map(lambda x:(x,0)).cache()
    links = vertices.leftOuterJoin(adjacency).mapValues(lambda p:p[1] or ()).cache()
    n = links.count()
    if n == 0:
        raise ValueError('Empty graph')
    ranks = vertices.mapValues(lambda _:1.0).cache()
    for iteration in range(1,11):
        joined = links.join(ranks).cache()
        dangling = joined.filter(lambda p:len(p[1][0])==0).map(lambda p:p[1][1]).sum()
        contributions = joined.flatMap(lambda p: [(v,p[1][1]/len(p[1][0])) for v in p[1][0]]).reduceByKey(add)
        updated = vertices.leftOuterJoin(contributions).mapValues(lambda p:0.15+0.85*((p[1] or 0.0)+dangling/n)).cache()
        mass = updated.values().sum()
        updated.sortByKey().map(lambda p:'%s\t%.17g'%p).saveAsTextFile(sys.argv[2]+'/iter%02d'%iteration)
        print('ITERATION %d COMPLETE mass=%.15f dangling=%.15f'%(iteration,mass,dangling),flush=True)
        ranks.unpersist(); joined.unpersist(); ranks=updated
    ranks.sortBy(lambda p:(-p[1],p[0]),numPartitions=1).map(lambda p:'(%s,%.10f)'%p).saveAsTextFile(sys.argv[2]+'/final')
    sc.stop()

if __name__ == '__main__':
    main()

