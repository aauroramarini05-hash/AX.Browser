@echo off
rem ---------------------------------------------------------------------------
rem Lightweight Gradle wrapper for Windows.
rem ---------------------------------------------------------------------------

set DIR=%~dp0
set WRAPPER_JAR=%DIR%gradle\wrapper\gradle-wrapper.jar

if exist "%WRAPPER_JAR%" if not %~z0==0 (
  set JAVA_CMD=java
  if defined JAVA_HOME set JAVA_CMD=%JAVA_HOME%\bin\java
  "%JAVA_CMD%" -Dorg.gradle.appname=AuryxBrowser -classpath "%WRAPPER_JAR%" org.gradle.wrapper.GradleWrapperMain %*
  goto:eof
)

rem Fallback to system gradle
where gradle >NUL 2>&1
if not errorlevel 1 (
  gradle %*
  goto:eof
)

echo ERROR: Could not find a usable Gradle installation.
echo Please install Gradle or regenerate the wrapper by running 'gradle wrapper' on a machine with Gradle installed.
exit /b 1