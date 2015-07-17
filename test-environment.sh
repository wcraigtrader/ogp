#! /bin/bash

echo ============================================================
echo Interesting environment variables:
env | egrep -i 'jdk|java|groovy' | sort

echo ============================================================
echo All Installed Java Versions:
ls -l /usr/lib/jvm 

echo ============================================================
echo All Build Tools:
ls -l /app/buildtools

echo ============================================================
echo Default Java version:
whereis java
java -version

echo ============================================================
echo "Java Home (${JAVA_HOME})"
if [ -x ${JAVA_HOME}/bin/java ] ; then
	${JAVA_HOME}/bin/java -version
fi

echo ============================================================
