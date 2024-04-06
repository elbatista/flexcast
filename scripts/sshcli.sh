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
clispernode=${12}
payload=${13}
thinktime=${14}
localm=${15}
dag_tree=${16}
region=${17}
# echo "clients=$clients ID=$ID duration=$duration algo=$algo locality=$locality warehouse=$warehouse msgs=$msgs log=$log tpcc=$tpcc clispernode=$clispernode" >> $basedir/logs/client$ID.txt

# for i in $(seq 1 $clispernode)
# do
    # echo "clients=$clients ID=$ID duration=$duration algo=$algo locality=$locality warehouse=$warehouse msgs=$msgs log=$log tpcc=$tpcc clispernode=$clispernode"
    ssh -o StrictHostKeyChecking=accept-new $node "cd $basedir; ./scripts/runClients.sh \
    $node $basedir $clients $ID $duration $algo $locality $warehouse $msgs $log $tpcc $clispernode $payload $thinktime $localm $dag_tree $region" &
    # java -cp "bin/*:lib/*" MainClient -c $clients -i $ID -d $duration -a $algo -l $locality -w $warehouse -m $msgs $log $tpcc >> $basedir/logs/client$ID.txt &
    # ID=$(($ID+1));
# done