#!/usr/bin/env sh
#
# Lightweight Gradle wrapper script.
#
# This script attempts to invoke the Gradle Wrapper if it exists, otherwise
# falls back to a locally installed Gradle binary. When used in environments
# such as GitHub Actions where a Gradle installation is provided, this
# lightweight wrapper will forward all commands to the installed Gradle. If
# neither the wrapper JAR nor a Gradle installation is available then the
# script will exit with an error.

set -e

# Resolve the directory of this script.
APP_HOME=$(cd "$(dirname "$0")"; pwd -P)

WRAPPER_JAR="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"

# Determine the Java command to use to start the JVM.
if [ -n "$JAVA_HOME" ]; then
  JAVA_CMD="$JAVA_HOME/bin/java"
else
  JAVA_CMD="java"
fi

if [ -f "$WRAPPER_JAR" ] && [ -s "$WRAPPER_JAR" ]; then
  # If the wrapper JAR exists and is non‑empty, use it to run the build.
  CLASSPATH="$WRAPPER_JAR"
  exec "$JAVA_CMD" -Dorg.gradle.appname="AuryxBrowser" -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
fi

# Fallback to system Gradle if available.
if command -v gradle >/dev/null 2>&1; then
  exec gradle "$@"
fi

echo "ERROR: Could not find a usable Gradle installation."
echo "Please install Gradle or regenerate the wrapper by running 'gradle wrapper' on a machine with Gradle installed."
exit 1