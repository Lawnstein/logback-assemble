@echo off
CHCP 65001
setlocal enabledelayedexpansion
title Start Testor
set RPATH=%~dp0

echo ============================
echo Start Test
echo ============================
if "%JAVA_HOME%"=="" (
set JAVA_HOME=D:\Java\jdk1.7.0_60
)
set PATH=%JAVA_HOME%\bin;%PATH%
set CLASSPATH=./;%CLASSPATH%;%JAVA_HOME%\lib;%JAVA_HOME%\lib\tools.jar;../lib/slf4j-api-1.7.20.jar;../lib/servlet-api-3.1.jar;../lib/log4j-1.2.8.jar;../lib/logback-access-1.1.7.jar;../lib/logback-classic-1.1.7.jar;../lib/logback-core-1.1.7.jar;
if "%JAVA_OPTS%"=="" (
	set JAVA_OPTS=-Xms64m -Xmx256m -XX:MaxPermSize=128M
)
echo %JAVA_OPTS%
java %JAVA_OPTS% -cp %CLASSPATH% test.ccbs.TestAssemble

pause