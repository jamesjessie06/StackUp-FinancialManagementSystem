#!/usr/bin/env bash
set -e
cd "$(dirname "$0")"
mkdir -p out
javac -encoding UTF-8 -d out src/backend/*.java src/frontend/*.java src/frontend/style/*.java src/Main.java
java -cp out Main
