#!/bin/sh
cd "$(dirname "$0")"
mkdir -p bin
javac -encoding UTF-8 -cp "lib/*" -d bin src/agri/*.java && java -cp "bin:lib/*" agri.Main
