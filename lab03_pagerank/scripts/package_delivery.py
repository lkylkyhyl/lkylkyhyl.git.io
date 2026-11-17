"""Package source, actual evidence and report; exclude caches and QA images."""
from pathlib import Path
import hashlib,json,zipfile
ROOT=Path(__file__).resolve().parents[1]
out=ROOT/'交付物'
files=[]
for folder in ('src','scripts','data','results','logs','交付物'):
    files.extend(p for p in (ROOT/folder).rglob('*') if p.is_file() and '__pycache__' not in p.parts and p.suffix not in ('.zip','.pyc','.crc') and p.name!='SHA256清单.json')
files.extend([ROOT/'README.md',ROOT/'.gitignore',ROOT/'.gitattributes'])
checks={p.relative_to(ROOT).as_posix():hashlib.sha256(p.read_bytes()).hexdigest() for p in sorted(files)}
manifest=out/'SHA256清单.json';manifest.write_text(json.dumps(checks,ensure_ascii=False,indent=2),encoding='utf-8')
files.append(manifest)
archive=out/'202400390065-侯亚琳-实验三完整交付.zip'
with zipfile.ZipFile(archive,'w',zipfile.ZIP_DEFLATED) as z:
    for p in sorted(files):z.write(p,'实验三/'+p.relative_to(ROOT).as_posix())
with zipfile.ZipFile(archive) as z:assert z.testzip() is None
print(archive)
print('files:',len(files),'bytes:',archive.stat().st_size)
