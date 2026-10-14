#!/bin/sh
APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar
exec java -Dsmilecare.app.home="$APP_HOME" -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
