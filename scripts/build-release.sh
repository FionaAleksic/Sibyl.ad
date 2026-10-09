#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"
VERSION="0.1.0-rc.1"
JAR_NAME="sibyl-ad-$VERSION.jar"
ZIP_NAME="Sibyl.ad-v$VERSION.zip"

# Only JDK 21 and zip are required; no network and no secret values.
command -v javac >/dev/null
command -v java >/dev/null
command -v jar >/dev/null
command -v zip >/dev/null
rm -rf build dist
mkdir -p build/main build/test build/stage dist

find src/main/java -type f -name '*.java' -print0 | xargs -0 javac --release 21 -Xlint:all -d build/main
find src/test/java -type f -name '*.java' -print0 | xargs -0 javac --release 21 -Xlint:all -cp build/main -d build/test
java -ea -cp build/main:build/test org.sibyl.addons.ad.AdConnectorTests

jar --create --file "dist/$JAR_NAME" -C build/main .
cp module.json config.schema.json LICENSE README.md build/stage/
cp "dist/$JAR_NAME" build/stage/
(cd build/stage && sha256sum module.json config.schema.json "$JAR_NAME" > SHA256SUMS.txt)
(cd build/stage && zip -q -X -r "../../dist/$ZIP_NAME" .)

# Fail build if the package does not follow the stable, versioned add-on format.
python3 - <<'PY'
import hashlib, json, pathlib, zipfile
path=pathlib.Path("dist/Sibyl.ad-v0.1.0-rc.1.zip")
with zipfile.ZipFile(path) as z:
    names=set(z.namelist())
    assert {"module.json","config.schema.json","SHA256SUMS.txt",
            "sibyl-ad-0.1.0-rc.1.jar","LICENSE","README.md"}.issubset(names)
    metadata=json.loads(z.read("module.json"))
    assert metadata["id"]=="Sibyl.ad"
    assert metadata["version"]=="0.1.0-rc.1"
    assert metadata["type"]=="dependency"
    assert metadata["runtime"]=="java21-jar"
    assert metadata["configurationSchema"]=="config.schema.json"
    checks=z.read("SHA256SUMS.txt").decode()
    for line in checks.splitlines():
        sha,name=line.split(maxsplit=1)
        assert hashlib.sha256(z.read(name)).hexdigest()==sha
print("Addon release ZIP verified")
PY

sha256sum "dist/$ZIP_NAME" > "dist/$ZIP_NAME.sha256"
echo "READY: dist/$ZIP_NAME"
