#! /bin/bash

validate_java() {
	varname=$1
	expanded_name=$2
	java_pathname=${expanded_name}/bin/java
	echo "Testing ${varname} => ${expanded_name} ... "
	if [ -d ${expanded_name} ] ; then
		if [ -x ${java_pathname} ] ; then
			${java_pathname} -version
		else 
			echo "No java executable found at ${java_pathname}"
		fi
	else 
		echo "Not actually installed at ${expanded_name}"
	fi
}

echo ============================================================
echo Interesting environment variables:
env | egrep -i 'jdk|java|groovy' | sort

echo ============================================================
echo All Build Tools:
ls -l /app/buildtools

echo ============================================================
echo Default Java version:
whereis java
java -version

echo ============================================================
echo Advertised versions of Java:
env | egrep -i "^jdk|^java" | sort | tr '=' ' ' | while read var pathname ; do
	validate_java $var $pathname
done

echo ============================================================
