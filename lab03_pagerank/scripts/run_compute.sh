#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
bash scripts/run_mr.sh
bash scripts/run_spark.sh
python3 scripts/verify_results.py > results/verification_stdout.txt
curl -fsS http://localhost:8088/ws/v1/cluster/apps > results/yarn_apps.json
echo ALL_COMPUTE_COMPLETE
