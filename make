#!/bin/sh

find | grep 'java$' | xargs javac -Xlint -Xlint:-serial -source 6 -target 6 -bootclasspath ../../rt.jar -extdirs "" -d bin/
jar cmf mainClass resolution.jar src/*.java -C bin/ .
cp -t sandbox resolution.jar


