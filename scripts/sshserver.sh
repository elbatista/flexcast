node=$1
basedir=$2
ID=$3
algo=$4
duration=$5
clients=$6
log=$7

ssh -o StrictHostKeyChecking=accept-new $node \
"cd $basedir; \
java -Xmx4024m -cp \"bin/*:lib/*\" MainServer -i $ID -a $algo -d $duration -c $clients $log >> $basedir/logs/node$ID.txt" & 