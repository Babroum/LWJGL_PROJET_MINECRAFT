@echo off
cd /d "%~dp0"
java ${launcher.jvmArgs} -jar "${project.build.finalName}.jar"
pause