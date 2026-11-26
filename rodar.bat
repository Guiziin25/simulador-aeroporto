@echo off
rem Uso: rodar.bat [config\C1.properties]
chcp 65001 >nul
set CONFIG=%1
if "%CONFIG%"=="" set CONFIG=config\C1.properties
if not exist out mkdir out
javac -encoding UTF-8 -d out src\aeroporto\*.java && java -ea -Dstdout.encoding=UTF-8 -cp out aeroporto.Principal %CONFIG%
