#!/usr/bin/env bash
# Явный отдельный шаг. Нужны docker login, Scout plugin и право на registry.
set -euo pipefail
cd "$(dirname "$0")"
image_ref="${1:?Укажите уже проверенный registry/repository:tag}"
evidence=target/release/image-evidence.json
expected_id=$(python3 - "$evidence" "$image_ref" <<'PY'
import json, sys
record = json.load(open(sys.argv[1]))
if record['imageRef'] != sys.argv[2] or record['integrityGate'] != 'passed':
    raise SystemExit('Нет integrity evidence для этого imageRef')
print(record['localImageId'])
PY
)
[[ "$(docker image inspect "$image_ref" --format '{{.Id}}')" == "$expected_id" ]]
# --exit-code возвращает 2 при находках; ошибка scanner также блокирует push.
docker scout cves --exit-code --only-severity critical,high "$image_ref" > target/release/cves.txt
# Повторно проверяем tag непосредственно перед push.
[[ "$(docker image inspect "$image_ref" --format '{{.Id}}')" == "$expected_id" ]]
docker push "$image_ref"
registry_digest=$(docker image inspect "$image_ref" --format '{{json .RepoDigests}}' | python3 -c '
import json, sys
repository = sys.argv[1].rsplit(":", 1)[0]
matching = [ref for ref in json.load(sys.stdin) if ref.startswith(repository + "@sha256:")]
if len(matching) != 1:
    raise SystemExit("Expected exactly one digest for published repository")
print(matching[0])
' "$image_ref")
python3 - "$evidence" "$registry_digest" <<'PY'
import json, re, sys
from pathlib import Path
path = Path(sys.argv[1]); record = json.loads(path.read_text())
if not re.fullmatch(r'.+@sha256:[a-f0-9]{64}', sys.argv[2]):
    raise SystemExit('Registry digest не получен')
record.update(vulnerabilityGate='passed', registryDigest=sys.argv[2])
path.write_text(json.dumps(record, indent=2) + '\n')
PY
