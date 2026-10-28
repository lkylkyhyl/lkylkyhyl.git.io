import os
import re
from collections import defaultdict

docs_dir = r'C:\Users\lrmwy\Desktop\大数据实验2\input_docs'
stopwords_file = r'C:\Users\lrmwy\Desktop\大数据实验2\stopwords.txt'

with open(stopwords_file, 'r', encoding='utf-8') as f:
    stopwords = set(line.strip().lower() for line in f if line.strip())

index = defaultdict(lambda: defaultdict(int))

for fname in sorted(os.listdir(docs_dir)):
    fpath = os.path.join(docs_dir, fname)
    with open(fpath, 'r', encoding='utf-8') as f:
        text = f.read().lower()
        words = re.findall(r'[a-zA-Z]+', text)
        for w in words:
            if len(w) >= 2 and w not in stopwords:
                index[w][fname] += 1

out_lines = []
for word in sorted(index.keys()):
    docs_dict = index[word]
    total = sum(docs_dict.values())
    parts = [f"<{doc},{cnt}>" for doc, cnt in sorted(docs_dict.items())]
    posting_str = ";".join(parts) + f";<total,{total}>."
    out_lines.append(f"{word}\t{posting_str}")

verify_out = r'C:\Users\lrmwy\Desktop\大数据实验2\local_verify_output.txt'
with open(verify_out, 'w', encoding='utf-8') as f:
    f.write('\n'.join(out_lines) + '\n')

print(f"Total indexed unique terms: {len(out_lines)}")
print("Sample output lines:")
for line in out_lines[:15]:
    print(line)
