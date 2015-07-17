#! /bin/bash

validate_java() {
	varname=$1
	dirname=${!varname}
	echo "Testing ${varname} => ${dirname} ... "
	if [ -d ${dirname} ] ; then
		if [ -x ${dirname}/bin/java ] ; then
			${dirname}/bin/java -version
		else 
			echo "No java executable found"
		fi
	else 
		echo "Not actually installed."
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
env | egrep -i "^jdk|^java" | cut -d'=' -f1 | sort | while read var ; do
	validate_java $var
done

echo ============================================================
