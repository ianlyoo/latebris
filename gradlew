#!/bin/sh

DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
WRAPPER_JAR="$DIR/gradle/wrapper/gradle-wrapper.jar"

if [ ! -f "$WRAPPER_JAR" ]; then
  echo "Gradle wrapper JAR is missing at $WRAPPER_JAR. Run 'gradle wrapper' from a machine with Gradle installed to finish bootstrap." >&2
  exit 1
fi

exec java -classpath "$WRAPPER_JAR" org.gradle.wrapper.GradleWrapperMain "$@"
