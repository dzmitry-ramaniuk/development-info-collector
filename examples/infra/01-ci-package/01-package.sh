#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
"${MVN_BIN:-mvn}" --batch-mode clean verify "$@"
mkdir -p target/release
cp target/app.jar target/release/app.jar
(cd target/release && sha256sum app.jar > app.jar.sha256)
printf '%s\n' 'Проверенный JAR и checksum: target/release/'
