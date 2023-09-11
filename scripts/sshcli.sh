node=$1
basedir=$2
clients=$3
ID=$4
duration=$5
algo=$6
locality=$7
warehouse=$8
msgs=$9
log=${10}
tpcc=${11}
ssh -o StrictHostKeyChecking=accept-new $node \
"cd $basedir; java -cp \"bin/*:lib/*\" MainClient -c $clients -i $ID -d $duration -a $algo -l $locality -w $warehouse -m $msgs $log $tpcc >> $basedir/logs/client$ID.txt" &
