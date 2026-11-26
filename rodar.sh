#!/bin/bash
# Uso: ./rodar.sh [config/C1.properties]
CONFIG=${1:-config/C1.properties}
mkdir -p out
javac -d out src/aeroporto/*.java && java -ea -Dstdout.encoding=UTF-8 -cp out aeroporto.Principal "$CONFIG"
