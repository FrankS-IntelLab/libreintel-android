#!/bin/bash
# Gradle wrapper script for LibreIntel Android app

cd "$(dirname "$0")"

# Check if JAVA_HOME is set, if not try to find Java
if [ -z "$JAVA_HOME" ]; then
    if [ -d "/usr/libexec/java_home" ]; then
        export JAVA_HOME=$(/usr/libexec/java_home -v 17)
    fi
fi

# Run gradle wrapper
exec ./gradlew "$@"