#!/bin/sh

rm -r bin/res
JAVAFILES=`find src | grep 'java$'`
javac -Xlint -Xlint:-serial -source 6 -target 6 -bootclasspath lib/rt.jar -extdirs "" -d bin/ $JAVAFILES 2>&1 | egrep --color "^|error"
jar cmf mainClass resolution.jar $JAVAFILES -C bin/ .
cp -t sandbox resolution.jar


