#!/bin/bash

# run example:
# ./scripts/runCluster.sh 120 0 150 6 12 95 0 100 25 false true true false 1
if [ "$#" -lt 14 ]; then 
    echo  "Usage: $0 <duration:sec> <algo:0-flex;1-skeen;2-byz> <#clis> <#servers> <#nodes> <locality> <#msgs> <#gc(ms)> <#clispernode> <tpcc> <payload> <thinktime> <localm> <dag_tree>"
    exit 0; 
fi

i=0;
ID=-1;
log="any"; # either -log or any
warehouse=0;
iniport=50000;
basedir=~/flexcast;
duration=$1;
algo=$2;
clients=$3;
servers=$4;
nodes=$5;
locality=$6;
msgs=$7;
gc=$8;
clispernode=$9;
tpcc=${10}
payload=${11}
thinktime=${12}
localm=${13}
dag_tree=${14}
rc="30"
iniNode=60
clilat=true

algodesc=("flexcast" "skeen" "byzcast")
rm -f -r $basedir/logs $basedir/files $basedir/results;
mkdir $basedir/logs; mkdir $basedir/files; mkdir $basedir/results;

echo false > $basedir/files/stop;
echo "------------------------------------------------------------------------------------------------" >> $basedir/logs/execution.log;
echo "started experiment on" $(date) >> $basedir/logs/execution.log;
echo "duration=$1 algo=${algodesc[$2]} clients=$3 servers=$4 nodes=$5 locality=$6 msgs=$7 gc=$8 \
clispernode=$9 tpcc=$tpcc payload=$payload thinktime=$thinktime" dag_tree=$dag_tree >> $basedir/logs/execution.log;
echo "------------------------------------------------------------------------------------------------" >> $basedir/logs/execution.log;

./scripts/killAll.sh $nodes $iniNode >> $basedir/logs/execution.log;

# compile, create config file, and update all other nodes
echo creating hosts.config for $servers servers >> $basedir/logs/execution.log;
echo "#id ip port" > $basedir/config/hosts.config;

confserverid=0;
serverfile=$basedir/config/servers.conf
while IFS=, read -r node region ip
do
    # echo "will deploy server id $confserverid on $node ($ip) representing region $region" >> $basedir/logs/execution.log;
    echo "$confserverid $ip $iniport" >> $basedir/config/hosts.config;
    confserverid=$(($confserverid+1));
    iniport=$(($iniport+10));
done < <( awk '!/^ *#/ && NF' "$serverfile");

cd $basedir;
echo compiling source code >> $basedir/logs/execution.log;
ant clean; ant;

sleep 1;

# one more client for the gc:
if [ "$gc" -gt 0 ]; then
    clients=$(($clients+1));
fi

# one more client for the reconfig client:
if [ "$rc" != "" ]; then 
    clients=$(($clients+1));
fi

# Start servers
declare -A warehouses
while IFS=, read -r node region ip
do
    ID=$(($ID+1));
    echo "starting server $ID on $node region $region" >> $basedir/logs/execution.log;
    warehouses[$node]=$ID;
    ./scripts/sshserver.sh $node $basedir $ID $algo $duration $clients $log $payload
    sleep .5;
done < <( awk '!/^ *#/ && NF' "$serverfile");
echo "started $servers servers"  >> $basedir/logs/execution.log;


lastnode="";
ID=0;
clifile=$basedir/config/clients.conf
while IFS=, read -r node region nodewarehouse
do
    warehouse="${warehouses[$nodewarehouse]}"
    echo "$clispernode clients on $node region $region assume as primary warehouse: $warehouse ($nodewarehouse)" >> $basedir/logs/execution.log;

    ./scripts/sshcli.sh $node $basedir $clients $ID $duration $algo $locality $warehouse $msgs $log $tpcc $clispernode $payload $thinktime $localm $dag_tree $region # >> $basedir/logs/execution.log;
    sleep 1;
    ID=$(($ID+$clispernode));
    lastnode=$node;
done < <( awk '!/^ *#/ && NF'  "$clifile");


if [ "$rc" != "" ]; then 
    ssh -o StrictHostKeyChecking=accept-new $lastnode \
    "cd $basedir; java -cp \"bin/*:lib/*\" MainClient -c $clients -i $ID -d $duration -a $algo $log -rc $rc >> logs/reconfigoracle.txt" &
    echo "started reconfig client ($rc sec) on $lastnode" >> $basedir/logs/execution.log;
    ID=$(($ID+1));
fi

if [ "$gc" -gt 0 ]; then
    echo "started $(($clients-1)) clients" >> $basedir/logs/execution.log;
    # ID=$(($ID+1));
    ssh -o StrictHostKeyChecking=accept-new $lastnode \
    "cd $basedir; java -cp \"bin/*:lib/*\" MainClient -c $clients -i $ID -d $duration -a $algo $log -gc $gc >> $basedir/logs/gc.txt" &
    echo started gc client on $lastnode >> $basedir/logs/execution.log;
else
    echo "started $clients clients" >> $basedir/logs/execution.log;
fi

echo "waiting for nodes to finish" >> $basedir/logs/execution.log;
sleep $duration;

while :
do
    nodeFiles=`find $basedir/files -name 'NodeFinished*' | wc -l` #Count files and store in a variable
    if [ "$nodeFiles" -ge $servers ]; then break; fi
    sleep 2;
done

echo "all nodes done" >> $basedir/logs/execution.log;
./scripts/killAll.sh $nodes $iniNode >> $basedir/logs/execution.log;
echo "experiment finished at " $(date)  >> $basedir/logs/execution.log;

expdir="$basedir/experiments/${algodesc[$2]}-reconfig/${servers}nodes/${3}cli/${locality}%/gc${gc}";

if [ "$rc" != "" ]; then
    expdir="$expdir/rc${rc}"
else
    expdir="$expdir/norc"
fi

if $clilat; then
    expdir="$expdir/clilat"
else
    expdir="$expdir/noclilat"
fi

mkdir -p $expdir/config
echo "moving data to" $expdir >> $basedir/logs/execution.log;
cp -r $basedir/logs $expdir/
cp -r $basedir/files $expdir/
cp -r $basedir/results $expdir/
cp    $basedir/config/*.conf* $expdir/config/

echo done. exiting  >> $basedir/logs/execution.log;

exit 0;















# mkdir $basedir/logs/nodes



#     # 3 nodes
#     if [ "$6" -eq 3 ]; then
#         #us-east-1 (virginia)
#         ssh elia@node3 "cd genbyzproto; java -Xmx4024m -cp \"bin/*:lib/*\" MainServer -i 0 -d $1 $dbg $sk $p $cpu $batch $timeout -c $5 >> logs/nodes/0-us-east-1-node3.txt" &
#         #eu-central-1 (frankfurt)
#         ssh elia@node7 "cd genbyzproto; java -Xmx4024m -cp \"bin/*:lib/*\" MainServer -i 1 -d $1 $dbg $sk $p $cpu $batch $timeout -c $5 >> logs/nodes/1-eu-central-1-node7.txt" &
#         #ap-northeast-1 (tokyo)
#         ssh elia@node11 "cd genbyzproto; java -Xmx4024m -cp \"bin/*:lib/*\" MainServer -i 2 -d $1 $dbg $sk $p $cpu $batch $timeout -c $5 >> logs/nodes/2-ap-northeast-1-node11.txt" &
#     fi
#     if [ "$6" -eq 6 ]; then
#         #us-west-1 (California)
#         ssh elia@node1 "cd genbyzproto;  java -Xmx4024m -cp \"bin/*:lib/*\" MainServer -i 0 -d $1 $dbg $sk $p $cpu $batch $timeout -c $5 >> logs/nodes/0-us-west-1-node1.txt" &
#         #us-east-1 (Virginia)
#         ssh elia@node3 "cd genbyzproto;  java -Xmx4024m -cp \"bin/*:lib/*\" MainServer -i 1 -d $1 $dbg $sk $p $cpu $batch $timeout -c $5 >> logs/nodes/1-us-east-1-node3.txt" &
#         #eu-west-1 (Ireland)
#         ssh elia@node5 "cd genbyzproto;  java -Xmx4024m -cp \"bin/*:lib/*\" MainServer -i 2 -d $1 $dbg $sk $p $cpu $batch $timeout -c $5 >> logs/nodes/2-eu-west-1-node5.txt" &
#         #eu-central-1 (Frankfurt)
#         ssh elia@node7 "cd genbyzproto;  java -Xmx4024m -cp \"bin/*:lib/*\" MainServer -i 3 -d $1 $dbg $sk $p $cpu $batch $timeout -c $5 >> logs/nodes/3-eu-central-1-node7.txt" &
#         #ap-south-1 (Mumbai)
#         ssh elia@node9 "cd genbyzproto;  java -Xmx4024m -cp \"bin/*:lib/*\" MainServer -i 4 -d $1 $dbg $sk $p $cpu $batch $timeout -c $5 >> logs/nodes/4-ap-south-1-node9.txt" &
#         #ap-northeast-1 (Tokyo)
#         ssh elia@node11 "cd genbyzproto; java -Xmx4024m -cp \"bin/*:lib/*\" MainServer -i 5 -d $1 $dbg $sk $p $cpu $batch $timeout -c $5 >> logs/nodes/5-ap-northeast-1-node11.txt" &
#     fi
#     if [ "$6" -eq 9 ]; then
#         #us-west-1 (California)
#         ssh elia@node1 "cd genbyzproto;  java -Xmx4024m -cp \"bin/*:lib/*\" MainServer -i 0 -d $1 $dbg $sk $p $cpu $batch $timeout -c $5 >> logs/nodes/0-us-west-1-node1.txt" &
#         #us-east-1 (Virginia)
#         ssh elia@node3 "cd genbyzproto;  java -Xmx4024m -cp \"bin/*:lib/*\" MainServer -i 1 -d $1 $dbg $sk $p $cpu $batch $timeout -c $5 >> logs/nodes/1-us-east-1-node3.txt" &
#         #sa-east-1 (Sao Paulo)
#         ssh elia@node4 "cd genbyzproto;  java -Xmx4024m -cp \"bin/*:lib/*\" MainServer -i 2 -d $1 $dbg $sk $p $cpu $batch $timeout -c $5 >> logs/nodes/2-sa-east-1-node4.txt" &
#         #eu-west-1 (Ireland)
#         ssh elia@node5 "cd genbyzproto;  java -Xmx4024m -cp \"bin/*:lib/*\" MainServer -i 3 -d $1 $dbg $sk $p $cpu $batch $timeout -c $5 >> logs/nodes/3-eu-west-1-node5.txt" &
#         #eu-central-1 (Frankfurt)
#         ssh elia@node7 "cd genbyzproto;  java -Xmx4024m -cp \"bin/*:lib/*\" MainServer -i 4 -d $1 $dbg $sk $p $cpu $batch $timeout -c $5 >> logs/nodes/4-eu-central-1-node7.txt" &
#         # eu-north-1 (Stockholm)
#         ssh elia@node8 "cd genbyzproto;  java -Xmx4024m -cp \"bin/*:lib/*\" MainServer -i 5 -d $1 $dbg $sk $p $cpu $batch $timeout -c $5 >> logs/nodes/5-eu-north-1-node8.txt" &
#         #ap-south-1 (Mumbai)
#         ssh elia@node9 "cd genbyzproto;  java -Xmx4024m -cp \"bin/*:lib/*\" MainServer -i 6 -d $1 $dbg $sk $p $cpu $batch $timeout -c $5 >> logs/nodes/6-ap-south-1-node9.txt" &
#         #ap-southeast-1  (Singapore)
#         ssh elia@node10 "cd genbyzproto; java -Xmx4024m -cp \"bin/*:lib/*\" MainServer -i 7 -d $1 $dbg $sk $p $cpu $batch $timeout -c $5 >> logs/nodes/7-ap-southeast-1-node10.txt" &
#         #ap-northeast-1 (Tokyo)
#         ssh elia@node11 "cd genbyzproto; java -Xmx4024m -cp \"bin/*:lib/*\" MainServer -i 8 -d $1 $dbg $sk $p $cpu $batch $timeout -c $5 >> logs/nodes/8-ap-northeast-1-node11.txt" &
#     fi
#     if [ "$6" -eq 12 ]; then
#         #us-west-1 (California)
#         ssh elia@node1 "cd genbyzproto;  java -Xmx4024m -cp \"bin/*:lib/*\" MainServer -i 0 -d $1 $dbg $sk $p $cpu $batch $timeout -c $5 >> logs/nodes/0-us-west-1-node1.txt" &
#         #ca-central-1 (Canada)
#         ssh elia@node2 "cd genbyzproto;  java -Xmx4024m -cp \"bin/*:lib/*\" MainServer -i 1 -d $1 $dbg $sk $p $cpu $batch $timeout -c $5 >> logs/nodes/1-ca-central-1-node2.txt" &
#         #us-east-1 (Virginia)
#         ssh elia@node3 "cd genbyzproto;  java -Xmx4024m -cp \"bin/*:lib/*\" MainServer -i 2 -d $1 $dbg $sk $p $cpu $batch $timeout -c $5 >> logs/nodes/2-us-east-1-node3.txt" &
#         #sa-east-1 (Sao Paulo)
#         ssh elia@node4 "cd genbyzproto;  java -Xmx4024m -cp \"bin/*:lib/*\" MainServer -i 3 -d $1 $dbg $sk $p $cpu $batch $timeout -c $5 >> logs/nodes/3-sa-east-1-node4.txt" &
#         #eu-west-1 (Ireland)
#         ssh elia@node5 "cd genbyzproto;  java -Xmx4024m -cp \"bin/*:lib/*\" MainServer -i 4 -d $1 $dbg $sk $p $cpu $batch $timeout -c $5 >> logs/nodes/4-eu-west-1-node5.txt" &
#         #eu-west-3 (Paris)
#         ssh elia@node6 "cd genbyzproto;  java -Xmx4024m -cp \"bin/*:lib/*\" MainServer -i 5 -d $1 $dbg $sk $p $cpu $batch $timeout -c $5 >> logs/nodes/5-eu-west-3-node6.txt" &
#         #eu-central-1 (Frankfurt)
#         ssh elia@node7 "cd genbyzproto;  java -Xmx4024m -cp \"bin/*:lib/*\" MainServer -i 6 -d $1 $dbg $sk $p $cpu $batch $timeout -c $5 >> logs/nodes/6-eu-central-1-node7.txt" &
#         # eu-north-1 (Stockholm)
#         ssh elia@node8 "cd genbyzproto;  java -Xmx4024m -cp \"bin/*:lib/*\" MainServer -i 7 -d $1 $dbg $sk $p $cpu $batch $timeout -c $5 >> logs/nodes/7-eu-north-1-node8.txt" &
#         #ap-south-1 (Mumbai)
#         ssh elia@node9 "cd genbyzproto;  java -Xmx4024m -cp \"bin/*:lib/*\" MainServer -i 8 -d $1 $dbg $sk $p $cpu $batch $timeout -c $5 >> logs/nodes/8-ap-south-1-node9.txt" &
#         #ap-southeast-1  (Singapore)
#         ssh elia@node10 "cd genbyzproto; java -Xmx4024m -cp \"bin/*:lib/*\" MainServer -i 9 -d $1 $dbg $sk $p $cpu $batch $timeout -c $5 >> logs/nodes/9-ap-southeast-1-node10.txt" &
#         #ap-northeast-1 (Tokyo)
#         ssh elia@node11 "cd genbyzproto; java -Xmx4024m -cp \"bin/*:lib/*\" MainServer -i 10 -d $1 $dbg $sk $p $cpu $batch $timeout -c $5 >> logs/nodes/10-ap-northeast-1-node11.txt" &
#         #ap-southeast-2 (Sydney)
#         ssh elia@node12 "cd genbyzproto; java -Xmx4024m -cp \"bin/*:lib/*\" MainServer -i 11 -d $1 $dbg $sk $p $cpu $batch $timeout -c $5 >> logs/nodes/11-ap-southeast-2-node12.txt" &
#     fi
#     echo "started $6 servers"  >> ~/genbyzproto/logs/executions.log

#     # Start clients
#     mkdir ~/genbyzproto/logs/clients
#     mkdir ~/genbyzproto/logs/clients/america
#     # cli-us-west-1 (California)
#     if [ "$6" -eq 3 ]; then warehouse=0; fi
#     if [ "$6" -eq 6 ]; then warehouse=0; fi
#     if [ "$6" -eq 9 ]; then warehouse=0; fi
#     if [ "$6" -eq 12 ]; then warehouse=0; fi
#     for i in $(seq 1 $clipernode); do
#         ssh elia@node15 "cd genbyzproto; java -cp \"bin/*:lib/*\" MainClient -c $5 -i $ID -d $1 $dbg $sk $tpcc $np $msgs $locality -w $warehouse -r america >> logs/clients/america/$ID-cli-us-west-1.txt" & sleep .05
#         ((ID = $ID + 1))
#     done
#     # cli-ca-central-1 (Canada)
#     if [ "$6" -eq 3 ]; then warehouse=0; fi
#     if [ "$6" -eq 6 ]; then warehouse=1; fi
#     if [ "$6" -eq 9 ]; then warehouse=1; fi
#     if [ "$6" -eq 12 ]; then warehouse=1; fi
#     for i in $(seq 1 $clipernode); do
#         ssh elia@node16 "cd genbyzproto; java -cp \"bin/*:lib/*\" MainClient -c $5 -i $ID -d $1 $dbg $sk $tpcc $np $msgs $locality -w $warehouse -r america >> logs/clients/america/$ID-cli-ca-central-1.txt" & sleep .05
#         ((ID = $ID + 1))
#     done
#     # cli-us-east-1 (Virginia)
#     if [ "$6" -eq 3 ]; then warehouse=0; fi
#     if [ "$6" -eq 6 ]; then warehouse=1; fi
#     if [ "$6" -eq 9 ]; then warehouse=1; fi
#     if [ "$6" -eq 12 ]; then warehouse=2; fi
#     for i in $(seq 1 $clipernode); do
#         ssh elia@node17 "cd genbyzproto; java -cp \"bin/*:lib/*\" MainClient -c $5 -i $ID -d $1 $dbg $sk $tpcc $np $msgs $locality -w $warehouse -r america >> logs/clients/america/$ID-cli-us-east-1.txt" & sleep .05
#         ((ID = $ID + 1))
#     done
#     # cli-sa-east-1 (Sao Paulo)
#     if [ "$6" -eq 3 ]; then warehouse=0; fi
#     if [ "$6" -eq 6 ]; then warehouse=1; fi
#     if [ "$6" -eq 9 ]; then warehouse=2; fi
#     if [ "$6" -eq 12 ]; then warehouse=3; fi
#     for i in $(seq 1 $clipernode); do
#         ssh elia@node18 "cd genbyzproto; java -cp \"bin/*:lib/*\" MainClient -c $5 -i $ID -d $1 $dbg $sk $tpcc $np $msgs $locality -w $warehouse -r america >> logs/clients/america/$ID-cli-sa-east-1.txt" & sleep .05
#         ((ID = $ID + 1))
#     done

#     mkdir ~/genbyzproto/logs/clients/europe
#     # cli-eu-west-1 (Ireland)
#     if [ "$6" -eq 3 ]; then warehouse=1; fi
#     if [ "$6" -eq 6 ]; then warehouse=2; fi # Ireland
#     if [ "$6" -eq 9 ]; then warehouse=3; fi # Ireland
#     if [ "$6" -eq 12 ]; then warehouse=4; fi
#     for i in $(seq 1 $clipernode); do
#         ssh elia@node19 "cd genbyzproto; java -cp \"bin/*:lib/*\" MainClient -c $5 -i $ID -d $1 $dbg $sk $tpcc $np $msgs $locality -w $warehouse -r europe >> logs/clients/europe/$ID-cli-eu-west-1.txt" & sleep .05
#         ((ID = $ID + 1))
#     done
#     # cli-eu-west-3 (Paris)
#     if [ "$6" -eq 3 ]; then warehouse=1; fi
#     if [ "$6" -eq 6 ]; then warehouse=2; fi # Ireland
#     if [ "$6" -eq 9 ]; then warehouse=3; fi # Ireland
#     if [ "$6" -eq 12 ]; then warehouse=5; fi
#     for i in $(seq 1 $clipernode); do
#         ssh elia@node20 "cd genbyzproto; java -cp \"bin/*:lib/*\" MainClient -c $5 -i $ID -d $1 $dbg $sk $tpcc $np $msgs $locality -w $warehouse -r europe >> logs/clients/europe/$ID-cli-eu-west-3.txt" & sleep .05
#         ((ID = $ID + 1))
#     done
#     # cli-eu-central-1 (Frankfurt)
#     if [ "$6" -eq 3 ]; then warehouse=1; fi
#     if [ "$6" -eq 6 ]; then warehouse=3; fi # Frankfurt
#     if [ "$6" -eq 9 ]; then warehouse=4; fi # Frankfurt
#     if [ "$6" -eq 12 ]; then warehouse=6; fi
#     for i in $(seq 1 $clipernode); do
#         ssh elia@node21 "cd genbyzproto; java -cp \"bin/*:lib/*\" MainClient -c $5 -i $ID -d $1 $dbg $sk $tpcc $np $msgs $locality -w $warehouse -r europe >> logs/clients/europe/$ID-cli-eu-central-1.txt" & sleep .05
#         ((ID = $ID + 1))
#     done
#     # cli-eu-north-1 (Stockholm)
#     if [ "$6" -eq 3 ]; then warehouse=1; fi
#     if [ "$6" -eq 6 ]; then warehouse=3; fi # Frankfurt
#     if [ "$6" -eq 9 ]; then warehouse=5; fi # Stockholm
#     if [ "$6" -eq 12 ]; then warehouse=7; fi
#     for i in $(seq 1 $clipernode); do
#         ssh elia@node22 "cd genbyzproto; java -cp \"bin/*:lib/*\" MainClient -c $5 -i $ID -d $1 $dbg $sk $tpcc $np $msgs $locality -w $warehouse -r europe >> logs/clients/europe/$ID-cli-eu-north-1.txt" & sleep .05
#         ((ID = $ID + 1))
#     done

#     mkdir ~/genbyzproto/logs/clients/asia
#     # ap-south-1 (Mumbai)
#     if [ "$6" -eq 3 ]; then warehouse=2; fi
#     if [ "$6" -eq 6 ]; then warehouse=4; fi
#     if [ "$6" -eq 9 ]; then warehouse=6; fi
#     if [ "$6" -eq 12 ]; then warehouse=8; fi
#     for i in $(seq 1 $clipernode); do
#         ssh elia@node23 "cd genbyzproto; java -cp \"bin/*:lib/*\" MainClient -c $5 -i $ID -d $1 $dbg $sk $tpcc $np $msgs $locality -w $warehouse -r asia >> logs/clients/asia/$ID-cli-ap-south-1.txt" & sleep .05
#         ((ID = $ID + 1))
#     done
#     # cli-ap-southeast-1 (Singapore)
#     if [ "$6" -eq 3 ]; then warehouse=2; fi
#     if [ "$6" -eq 6 ]; then warehouse=4; fi
#     if [ "$6" -eq 9 ]; then warehouse=7; fi
#     if [ "$6" -eq 12 ]; then warehouse=9; fi
#     for i in $(seq 1 $clipernode); do
#         ssh elia@node24 "cd genbyzproto; java -cp \"bin/*:lib/*\" MainClient -c $5 -i $ID -d $1 $dbg $sk $tpcc $np $msgs $locality -w $warehouse -r asia >> logs/clients/asia/$ID-cli-ap-southeast-1.txt" & sleep .05
#         ((ID = $ID + 1))
#     done
#     # cli-ap-northeast-1 (Tokyo)
#     if [ "$6" -eq 3 ]; then warehouse=2; fi
#     if [ "$6" -eq 6 ]; then warehouse=5; fi
#     if [ "$6" -eq 9 ]; then warehouse=8; fi
#     if [ "$6" -eq 12 ]; then warehouse=10; fi
#     for i in $(seq 1 $clipernode); do
#         ssh elia@node25 "cd genbyzproto; java -cp \"bin/*:lib/*\" MainClient -c $5 -i $ID -d $1 $dbg $sk $tpcc $np $msgs $locality -w $warehouse -r asia >> logs/clients/asia/$ID-cli-ap-northeast-1.txt" & sleep .05
#         ((ID = $ID + 1))
#     done
#     # cli-ap-southeast-2 (Sydney)
#     if [ "$6" -eq 3 ]; then warehouse=2; fi
#     if [ "$6" -eq 6 ]; then warehouse=5; fi
#     if [ "$6" -eq 9 ]; then warehouse=8; fi
#     if [ "$6" -eq 12 ]; then warehouse=11; fi
#     for i in $(seq 1 $clipernode); do
#         ssh elia@node26 "cd genbyzproto; java -cp \"bin/*:lib/*\" MainClient -c $5 -i $ID -d $1 $dbg $sk $tpcc $np $msgs $locality -w $warehouse -r asia >> logs/clients/asia/$ID-cli-ap-southeast-2.txt" & sleep .05
#         ((ID = $ID + 1))
#     done
#     echo "started $5 clients" >> ~/genbyzproto/logs/executions.log

#     echo "waiting..." >> ~/genbyzproto/logs/executions.log
#     while :
#     do
#         sleep 2;
#         nodeFiles=`find ./files -name 'NodeFinished*' | wc -l` #Count files and store in a variable
#         if [ "$nodeFiles" -ge $6 ]; then sleep 5; break; fi
#     done

#     echo "all nodes done" >> ~/genbyzproto/logs/executions.log; ./scripts/kill.sh; echo "processes killed at " $(date)  >> ~/genbyzproto/logs/executions.log; sleep 1;

#     if grep -q "true" ~/genbyzproto/files/stop; then echo "found stop, exiting..." >> ~/genbyzproto/logs/executions.log; break; fi
#     echo "all queues empty !"  >> ~/genbyzproto/logs/executions.log; sleep 1;

#     echo "starting cycle validation ("$(date)")" >> ~/genbyzproto/logs/executions.log; java -Xmx4024m -cp "bin/*:lib/*" util.Validator > ~/genbyzproto/logs/validationresult.txt;
#     cat ~/genbyzproto/logs/validationresult.txt >> ~/genbyzproto/logs/executions.log;
#     if grep -q "true" ~/genbyzproto/logs/validationresult.txt; then echo "cycle detected!" >> ~/genbyzproto/logs/executions.log; break; fi
#     echo "no cycles detected ("$(date)")" >> ~/genbyzproto/logs/executions.log;

