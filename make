#!/bin/sh

JAVAFILES=`find src | grep 'java$'`
javac -Xlint -Xlint:-serial -source 6 -target 6 -bootclasspath lib/rt.jar -extdirs "" -d bin/ $JAVAFILES
jar cmf mainClass resolution.jar $JAVAFILES -C bin/ .
cp -t sandbox resolution.jar


