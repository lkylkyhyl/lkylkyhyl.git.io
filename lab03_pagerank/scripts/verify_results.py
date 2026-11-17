"""Independent Decimal oracle verifies all ten MR/Spark iteration snapshots."""
from decimal import Decimal, getcontext
from pathlib import Path
import json, re
getcontext().prec = 50
ROOT = Path(__file__).resolve().parents[1]

def load_graph(path):
    graph = {}
    for line in path.read_text(encoding='utf-8').splitlines():
        if not line.strip(): continue
        page, links = line.split('\t')
        graph.setdefault(page.strip(), set()).update(v.strip() for v in links.split(',') if v.strip())
    for v in list({v for links in graph.values() for v in links}): graph.setdefault(v,set())
    return graph

def oracle(graph):
    n = Decimal(len(graph)); d = Decimal('0.85')
    ranks = {v: Decimal(1) for v in graph}; history = []
    for _ in range(10):
        dangling = sum((ranks[v] for v in graph if not graph[v]), Decimal(0))
        updated = {v:1-d+d*dangling/n for v in graph}
        for u, links in graph.items():
            for v in links: updated[v] += d*ranks[u]/len(links)
        ranks = updated; history.append(ranks)
    return history

def main():
    graph = load_graph(ROOT/'data/graph.tsv'); history = oracle(graph); summary=[]
    for i, expected in enumerate(history,1):
        row={'iteration':i,'oracle_mass':float(sum(expected.values()))}
        for framework in ('mr','spark'):
            lines=(ROOT/('results/%s_iter%02d.tsv'%(framework,i))).read_text().splitlines()
            actual={p[0]:Decimal(p[1]) for p in (line.split('\t') for line in lines)}
            assert set(actual)==set(expected), (framework,i,'vertex mismatch')
            error=max(abs(actual[v]-expected[v]) for v in expected)
            assert error < Decimal('1e-12'), (framework,i,error)
            assert abs(sum(actual.values())-len(graph))<Decimal('1e-12')
            row[framework+'_max_error']=float(error)
        summary.append(row)
    outputs=[]
    for name in ('mapreduce','spark'):
        lines=(ROOT/('results/'+name+'.txt')).read_text().splitlines()
        assert all(re.fullmatch(r'\([^,]+,\d+\.\d{10}\)',s) for s in lines)
        records=[(s[1:].split(',')[0],Decimal(s.split(',')[1][:-1])) for s in lines]
        assert len(records)==len(graph) and set(dict(records))==set(graph)
        assert records==sorted(records,key=lambda p:(-p[1],p[0]))
        for v,r in records: assert abs(r-history[-1][v])<=Decimal('5.1e-11')
        outputs.append(lines)
    assert outputs[0]==outputs[1], 'Formatted final outputs differ'
    # Mathematical invariants for a self loop, all dangling vertices and a cycle.
    for case in ({'A':{'A'}},{'A':set(),'B':set()},{'A':{'B'},'B':{'A'}}):
        assert all(abs(v-1)<Decimal('1e-45') for v in oracle(case)[-1].values())
    result={'passed':True,'vertices':len(graph),'edges':sum(map(len,graph.values())),
            'iterations':summary,'final_outputs_identical':True}
    (ROOT/'results/verification.json').write_text(json.dumps(result,indent=2)+'\n')
    print(json.dumps(result,indent=2))

if __name__=='__main__': main()
