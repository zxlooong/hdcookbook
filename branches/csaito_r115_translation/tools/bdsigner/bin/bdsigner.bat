@echo off


rem %~dp0 is expanded pathname of the current script under NT
set BDSIGNER_HOME=%~dp0..

if "%JAVA_HOME%" == "" goto noJavaHome
  %JAVA_HOME%/bin/java -cp %BDSIGNER_HOME%/build/bdsigner.jar;%JAVA_HOME%/lib/tools.jar%BDSIGNER_HOME%/resource/bcprov-jdk15-137.jar sun.security.tools.JarSigner %*
  goto end
:noJavaHome
  echo Please set JAVA_HOME before running this script
  goto end
:end