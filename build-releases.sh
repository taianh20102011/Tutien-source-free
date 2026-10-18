#!/usr/bin/env sh
set -eu
ROOT="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
cd "$ROOT"
mkdir -p dist
mvn clean package -Pfree
cp target/TuTienPlugin-free-7.7.2.jar dist/
printf "\nFREE JAR created in %s/dist\n" "$ROOT"
ls -lh dist/TuTienPlugin-free-7.7.2.jar
