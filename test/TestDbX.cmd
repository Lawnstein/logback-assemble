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
set CLASSPATH=./;%CLASSPATH%;%JAVA_HOME%\lib;%JAVA_HOME%\lib\tools.jar;../lib/jcl-over-slf4j-1.6.4.jar;../lib/logback-access-1.1.7.jar;../lib/logback-classic-1.1.7.jar;../lib/logback-core-1.1.7.jar;../lib/slf4j-api-1.7.16.jar;
if "%JAVA_OPTS%"=="" (
	set JAVA_OPTS=-Xms64m -Xmx256m -XX:MaxPermSize=128M
)
echo %JAVA_OPTS%
java %JAVA_OPTS% -cp %CLASSPATH% test.ccbs.TestDbX

pause