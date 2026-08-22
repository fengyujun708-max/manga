#!/bin/sh
# 全源回归：explore + search
cd /var/minis/workspace/manga/tools/harness
for f in js/*.js; do
  name=$(basename "$f" .js)
  exp=$(timeout 40 node run.js "$f" explore 2>&1)
  ok_exp=$(echo "$exp" | grep -c '"error"' || true)
  items=$(echo "$exp" | python3 -c "
import json,sys
try:
    d=json.loads(sys.stdin.read())
    if isinstance(d,list):
        tot=sum(int(s.get('items','0') if isinstance(s.get('items'),str) and s.get('items','').endswith('条') else 0) for s in d)
        errs=[s['error'] for s in d if s.get('error')]
        print(f'{tot}items err={errs[:1]}' if errs else f'{tot}items OK')
    else: print('FATAL',d.get('fatal'))
except Exception as e: print('PARSE_FAIL')
")
  echo "$name | $items"
done
