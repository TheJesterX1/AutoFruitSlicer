#!/bin/sh
# Gradle wrapper script — Android Studio/GitHub Actions uses this to bootstrap Gradle
exec "$JAVA_HOME/bin/java" -jar "$(dirname "$0")/gradle/wrapper/gradle-wrapper.jar" "$@"
