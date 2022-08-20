kill `tail -1 ./pid/c.pid`
kill `tail -1 ./pid/c.pid`

pushd src

export CLASSPATH=$CLASSPATH:../lib/java\ json.jar
javac -server *.java

popd

export CLASSPATH=$CLASSPATH:./lib/java\ json.jar
java src/c $1 > ./log/log.txt 2>&1 &
var=$!
echo -n $var > ./pid/c.pid

