#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
image_ref="${1:-infra-order-total:local}"
runtime_ref="${RUNTIME_IMAGE:-eclipse-temurin:21-jre}"
rm -f target/release/image-evidence.json
(cd target/release && sha256sum -c app.jar.sha256)
# Tag разрешается один раз; Dockerfile получает immutable base reference.
docker pull "$runtime_ref"
runtime_digest=$(docker image inspect "$runtime_ref" --format '{{index .RepoDigests 0}}')
[[ "$runtime_digest" =~ @sha256:[a-f0-9]{64}$ ]]
docker build --build-arg "RUNTIME_IMAGE=$runtime_digest" -f Dockerfile -t "$image_ref" target/release
image_id=$(docker image inspect "$image_ref" --format '{{.Id}}')
[[ "$(docker image inspect "$image_id" --format '{{.Config.User}}')" == '10001:10001' ]]
# Gate извлекает bytes из готового image, не только проверяет исходный JAR.
gate_dir=$(mktemp -d)
container_id=''
cleanup() {
  if [[ -n "$container_id" ]]; then docker rm -f "$container_id" >/dev/null; fi
  rm -rf "$gate_dir"
}
trap cleanup EXIT
container_id=$(docker create "$image_id" 1200 3)
docker cp "$container_id:/app/app.jar" "$gate_dir/app.jar"
expected_sha=$(awk '{print $1}' target/release/app.jar.sha256)
[[ "$(sha256sum "$gate_dir/app.jar" | awk '{print $1}')" == "$expected_sha" ]]
[[ "$(docker run --rm --read-only --cap-drop ALL --security-opt no-new-privileges "$image_id" 1200 3)" == '3600' ]]
set +e
docker run --rm --read-only --cap-drop ALL --security-opt no-new-privileges "$image_id" 1200 0 >"$gate_dir/negative.out" 2>&1
negative_status=$?
set -e
[[ "$negative_status" == 2 ]]
python3 - "$image_ref" "$image_id" "$runtime_digest" "$expected_sha" <<'PY'
import json, os, sys
from pathlib import Path
ref, image_id, base, jar_sha = sys.argv[1:]
Path('target/release/image-evidence.json').write_text(json.dumps({
    'imageRef': ref, 'localImageId': image_id, 'runtimeBaseDigest': base,
    'jarSha256': jar_sha, 'sourceCommit': os.environ.get('GITHUB_SHA', 'local-working-tree'),
    'integrityGate': 'passed', 'vulnerabilityGate': 'not-run',
    'registryDigest': None,
}, indent=2) + '\n')
PY
printf '%s\n' 'Integrity/runtime gate passed; image-evidence.json создан. Публикация не выполнялась.'
