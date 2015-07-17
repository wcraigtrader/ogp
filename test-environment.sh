#! /bin/bash

echo Interesting environment variables:
env | egrep -i 'jdk|java|groovy'

echo Default Java version:
whereis java
java -version

echo "Java Home (${JAVA_HOME})"
if [ -x ${JAVA_HOME)/bin/java ] ; then
	${JAVA_HOME}/bin/java -version
fi