if [ "$#" -lt 8 ]; then echo "Usage: $0 <duration:sec> <debug:bool> <skeen:bool> <tpcc:bool> <#clis> <#servers> <latency:ms> <#experiments> <#partitions> <pfon:bool> <cpu:bool> <#msgs> <batch:bool> <batchtimeout:nanos> <%locality> <#clispernode>"; exit 0; fi
i=0; dbg=""; sk="-sk $3"; tpcc=""; np=""; p=""; cpu=""; msgs=""; timeout=""; batch=""; locality="", clipernode=""; ID=0; warehouse=0;
if [ "$2" == "true" ]; then dbg="-dbg"; fi
if [ "$4" == "true" ]; then tpcc="-t"; fi
if [ "$#" -gt 8 ]; then np="-np $9"; fi
if [ "$#" -gt 9 ]; then if [ "${10}" == "true" ]; then p="-p"; fi fi
if [ "$#" -gt 10 ]; then if [ "${11}" == "true" ]; then cpu="-cpu"; fi fi
if [ "$#" -gt 11 ]; then msgs="-m ${12}"; fi
if [ "$#" -gt 12 ]; then if [ "${13}" == "true" ]; then batch="-bs 2"; fi fi
if [ "$#" -gt 13 ]; then timeout="-bt ${14}"; fi
if [ "$#" -gt 14 ]; then locality="-l ${15}"; fi
if [ "$#" -gt 15 ]; then clipernode="${16}"; fi
rm -f -r ~/genbyzproto/logs/*  ~/genbyzproto/files/*; ./scripts/kill.sh;

for exe in $(seq 1 $8); do
    rm -f -r ~/genbyzproto/logs/*.txt  ~/genbyzproto/files/* ~/genbyzproto/results/*; echo false > ~/genbyzproto/files/stop;
    echo "------------------------------------------------------------------------------------------------" >> ~/genbyzproto/logs/executions.log
    echo "execution " $exe " at " $(date) >> ~/genbyzproto/logs/executions.log
    echo $0 $1 $2 $3 $4 $5 $6 $7 $8 $9 ${10} $cpu $msgs $batch $timeout $locality >> ~/genbyzproto/logs/executions.log
    echo "------------------------------------------------------------------------------------------------" >> ~/genbyzproto/logs/executions.log

    # Start servers
    mkdir ~/genbyzproto/logs/nodes

    if [ "$6" -eq 6 ]; then

        #eu-central-1 (Frankfurt)
        ssh elia@node7 "cd genbyzproto;  java -Xmx4024m -cp \"bin/*:lib/*\" MainServer -i 0 -d $1 $dbg -a 2 $p $cpu $batch $timeout -c $5 >> logs/nodes/0-eu-central-1-node7.txt" &
        #eu-west-1 (Ireland)
        ssh elia@node5 "cd genbyzproto;  java -Xmx4024m -cp \"bin/*:lib/*\" MainServer -i 1 -d $1 $dbg -a 2 $p $cpu $batch $timeout -c $5 >> logs/nodes/1-eu-west-1-node5.txt" &
        #ap-south-1 (Mumbai)
        ssh elia@node9 "cd genbyzproto;  java -Xmx4024m -cp \"bin/*:lib/*\" MainServer -i 2 -d $1 $dbg -a 2 $p $cpu $batch $timeout -c $5 >> logs/nodes/2-ap-south-1-node9.txt" &
        #us-east-1 (Virginia)
        ssh elia@node3 "cd genbyzproto;  java -Xmx4024m -cp \"bin/*:lib/*\" MainServer -i 3 -d $1 $dbg -a 2 $p $cpu $batch $timeout -c $5 >> logs/nodes/3-us-east-1-node3.txt" &
        #us-west-1 (California)
        ssh elia@node13 "cd genbyzproto;  java -Xmx4024m -cp \"bin/*:lib/*\" MainServer -i 4 -d $1 $dbg -a 2 $p $cpu $batch $timeout -c $5 >> logs/nodes/4-us-west-1-node13.txt" &
        #ap-northeast-1 (Tokyo)
        ssh elia@node11 "cd genbyzproto; java -Xmx4024m -cp \"bin/*:lib/*\" MainServer -i 5 -d $1 $dbg -a 2 $p $cpu $batch $timeout -c $5 >> logs/nodes/5-ap-northeast-1-node11.txt" &

    fi
    if [ "$6" -eq 9 ]; then
        #us-west-1 (California)
        ssh elia@node1 "cd genbyzproto;  java -Xmx4024m -cp \"bin/*:lib/*\" MainServer -i 0 -d $1 $dbg $sk $p $cpu $batch $timeout -c $5 >> logs/nodes/0-us-west-1-node1.txt" &
        #us-east-1 (Virginia)
        ssh elia@node3 "cd genbyzproto;  java -Xmx4024m -cp \"bin/*:lib/*\" MainServer -i 1 -d $1 $dbg $sk $p $cpu $batch $timeout -c $5 >> logs/nodes/1-us-east-1-node3.txt" &
        #sa-east-1 (Sao Paulo)
        ssh elia@node4 "cd genbyzproto;  java -Xmx4024m -cp \"bin/*:lib/*\" MainServer -i 2 -d $1 $dbg $sk $p $cpu $batch $timeout -c $5 >> logs/nodes/2-sa-east-1-node4.txt" &
        #eu-west-1 (Ireland)
        ssh elia@node5 "cd genbyzproto;  java -Xmx4024m -cp \"bin/*:lib/*\" MainServer -i 3 -d $1 $dbg $sk $p $cpu $batch $timeout -c $5 >> logs/nodes/3-eu-west-1-node5.txt" &
        #eu-central-1 (Frankfurt)
        ssh elia@node7 "cd genbyzproto;  java -Xmx4024m -cp \"bin/*:lib/*\" MainServer -i 4 -d $1 $dbg $sk $p $cpu $batch $timeout -c $5 >> logs/nodes/4-eu-central-1-node7.txt" &
        # eu-north-1 (Stockholm)
        ssh elia@node8 "cd genbyzproto;  java -Xmx4024m -cp \"bin/*:lib/*\" MainServer -i 5 -d $1 $dbg $sk $p $cpu $batch $timeout -c $5 >> logs/nodes/5-eu-north-1-node8.txt" &
        #ap-south-1 (Mumbai)
        ssh elia@node9 "cd genbyzproto;  java -Xmx4024m -cp \"bin/*:lib/*\" MainServer -i 6 -d $1 $dbg $sk $p $cpu $batch $timeout -c $5 >> logs/nodes/6-ap-south-1-node9.txt" &
        #ap-southeast-1  (Singapore)
        ssh elia@node10 "cd genbyzproto; java -Xmx4024m -cp \"bin/*:lib/*\" MainServer -i 7 -d $1 $dbg $sk $p $cpu $batch $timeout -c $5 >> logs/nodes/7-ap-southeast-1-node10.txt" &
        #ap-northeast-1 (Tokyo)
        ssh elia@node11 "cd genbyzproto; java -Xmx4024m -cp \"bin/*:lib/*\" MainServer -i 8 -d $1 $dbg $sk $p $cpu $batch $timeout -c $5 >> logs/nodes/8-ap-northeast-1-node11.txt" &
    fi
    if [ "$6" -eq 12 ]; then
        #us-west-1 (California)
        ssh elia@node1 "cd genbyzproto;  java -Xmx4024m -cp \"bin/*:lib/*\" MainServer -i 0 -d $1 $dbg $sk $p $cpu $batch $timeout -c $5 >> logs/nodes/0-us-west-1-node1.txt" &
        #ca-central-1 (Canada)
        ssh elia@node2 "cd genbyzproto;  java -Xmx4024m -cp \"bin/*:lib/*\" MainServer -i 1 -d $1 $dbg $sk $p $cpu $batch $timeout -c $5 >> logs/nodes/1-ca-central-1-node2.txt" &
        #us-east-1 (Virginia)
        ssh elia@node3 "cd genbyzproto;  java -Xmx4024m -cp \"bin/*:lib/*\" MainServer -i 2 -d $1 $dbg $sk $p $cpu $batch $timeout -c $5 >> logs/nodes/2-us-east-1-node3.txt" &
        #sa-east-1 (Sao Paulo)
        ssh elia@node4 "cd genbyzproto;  java -Xmx4024m -cp \"bin/*:lib/*\" MainServer -i 3 -d $1 $dbg $sk $p $cpu $batch $timeout -c $5 >> logs/nodes/3-sa-east-1-node4.txt" &
        #eu-west-1 (Ireland)
        ssh elia@node5 "cd genbyzproto;  java -Xmx4024m -cp \"bin/*:lib/*\" MainServer -i 4 -d $1 $dbg $sk $p $cpu $batch $timeout -c $5 >> logs/nodes/4-eu-west-1-node5.txt" &
        #eu-west-3 (Paris)
        ssh elia@node6 "cd genbyzproto;  java -Xmx4024m -cp \"bin/*:lib/*\" MainServer -i 5 -d $1 $dbg $sk $p $cpu $batch $timeout -c $5 >> logs/nodes/5-eu-west-3-node6.txt" &
        #eu-central-1 (Frankfurt)
        ssh elia@node7 "cd genbyzproto;  java -Xmx4024m -cp \"bin/*:lib/*\" MainServer -i 6 -d $1 $dbg $sk $p $cpu $batch $timeout -c $5 >> logs/nodes/6-eu-central-1-node7.txt" &
        # eu-north-1 (Stockholm)
        ssh elia@node8 "cd genbyzproto;  java -Xmx4024m -cp \"bin/*:lib/*\" MainServer -i 7 -d $1 $dbg $sk $p $cpu $batch $timeout -c $5 >> logs/nodes/7-eu-north-1-node8.txt" &
        #ap-south-1 (Mumbai)
        ssh elia@node9 "cd genbyzproto;  java -Xmx4024m -cp \"bin/*:lib/*\" MainServer -i 8 -d $1 $dbg $sk $p $cpu $batch $timeout -c $5 >> logs/nodes/8-ap-south-1-node9.txt" &
        #ap-southeast-1  (Singapore)
        ssh elia@node10 "cd genbyzproto; java -Xmx4024m -cp \"bin/*:lib/*\" MainServer -i 9 -d $1 $dbg $sk $p $cpu $batch $timeout -c $5 >> logs/nodes/9-ap-southeast-1-node10.txt" &
        #ap-northeast-1 (Tokyo)
        ssh elia@node11 "cd genbyzproto; java -Xmx4024m -cp \"bin/*:lib/*\" MainServer -i 10 -d $1 $dbg $sk $p $cpu $batch $timeout -c $5 >> logs/nodes/10-ap-northeast-1-node11.txt" &
        #ap-southeast-2 (Sydney)
        ssh elia@node12 "cd genbyzproto; java -Xmx4024m -cp \"bin/*:lib/*\" MainServer -i 11 -d $1 $dbg $sk $p $cpu $batch $timeout -c $5 >> logs/nodes/11-ap-southeast-2-node12.txt" &
    fi
    echo "started $6 servers"  >> ~/genbyzproto/logs/executions.log

    # Start clients
    mkdir ~/genbyzproto/logs/clients
    mkdir ~/genbyzproto/logs/clients/america
    # cli-us-west-1 (California)
    if [ "$6" -eq 6 ]; then warehouse=4; fi
    for i in $(seq 1 $clipernode); do
        ssh elia@node15 "cd genbyzproto; java -cp \"bin/*:lib/*\" MainClient -c $5 -i $ID -d $1 $dbg -a 2 $tpcc $np $msgs $locality -w $warehouse -r america >> logs/clients/america/$ID-cli-us-west-1.txt" & sleep .05
        ((ID = $ID + 1))
    done
    # cli-ca-central-1 (Canada)
    if [ "$6" -eq 6 ]; then warehouse=3; fi
    for i in $(seq 1 $clipernode); do
        ssh elia@node16 "cd genbyzproto; java -cp \"bin/*:lib/*\" MainClient -c $5 -i $ID -d $1 $dbg -a 2 $tpcc $np $msgs $locality -w $warehouse -r america >> logs/clients/america/$ID-cli-ca-central-1.txt" & sleep .05
        ((ID = $ID + 1))
    done
    # cli-us-east-1 (Virginia)
    if [ "$6" -eq 6 ]; then warehouse=3; fi
    for i in $(seq 1 $clipernode); do
        ssh elia@node27 "cd genbyzproto; java -cp \"bin/*:lib/*\" MainClient -c $5 -i $ID -d $1 $dbg -a 2 $tpcc $np $msgs $locality -w $warehouse -r america >> logs/clients/america/$ID-cli-us-east-1.txt" & sleep .05
        ((ID = $ID + 1))
    done
    # cli-sa-east-1 (Sao Paulo)
    if [ "$6" -eq 6 ]; then warehouse=3; fi
    for i in $(seq 1 $clipernode); do
        ssh elia@node18 "cd genbyzproto; java -cp \"bin/*:lib/*\" MainClient -c $5 -i $ID -d $1 $dbg -a 2 $tpcc $np $msgs $locality -w $warehouse -r america >> logs/clients/america/$ID-cli-sa-east-1.txt" & sleep .05
        ((ID = $ID + 1))
    done

    mkdir ~/genbyzproto/logs/clients/europe
    # cli-eu-west-1 (Ireland)
    if [ "$6" -eq 6 ]; then warehouse=1; fi # Ireland
    for i in $(seq 1 $clipernode); do
        ssh elia@node19 "cd genbyzproto; java -cp \"bin/*:lib/*\" MainClient -c $5 -i $ID -d $1 $dbg -a 2 $tpcc $np $msgs $locality -w $warehouse -r europe >> logs/clients/europe/$ID-cli-eu-west-1.txt" & sleep .05
        ((ID = $ID + 1))
    done
    # cli-eu-west-3 (Paris)
    if [ "$6" -eq 6 ]; then warehouse=1; fi # Ireland
    for i in $(seq 1 $clipernode); do
        ssh elia@node28 "cd genbyzproto; java -cp \"bin/*:lib/*\" MainClient -c $5 -i $ID -d $1 $dbg -a 2 $tpcc $np $msgs $locality -w $warehouse -r europe >> logs/clients/europe/$ID-cli-eu-west-3.txt" & sleep .05
        ((ID = $ID + 1))
    done
    # cli-eu-central-1 (Frankfurt)
    if [ "$6" -eq 6 ]; then warehouse=0; fi # Frankfurt
    for i in $(seq 1 $clipernode); do
        ssh elia@node21 "cd genbyzproto; java -cp \"bin/*:lib/*\" MainClient -c $5 -i $ID -d $1 $dbg -a 2 $tpcc $np $msgs $locality -w $warehouse -r europe >> logs/clients/europe/$ID-cli-eu-central-1.txt" & sleep .05
        ((ID = $ID + 1))
    done
    # cli-eu-north-1 (Stockholm)
    if [ "$6" -eq 6 ]; then warehouse=0; fi # Frankfurt
    for i in $(seq 1 $clipernode); do
        ssh elia@node22 "cd genbyzproto; java -cp \"bin/*:lib/*\" MainClient -c $5 -i $ID -d $1 $dbg -a 2 $tpcc $np $msgs $locality -w $warehouse -r europe >> logs/clients/europe/$ID-cli-eu-north-1.txt" & sleep .05
        ((ID = $ID + 1))
    done

    mkdir ~/genbyzproto/logs/clients/asia
    # ap-south-1 (Mumbai)
    if [ "$6" -eq 6 ]; then warehouse=2; fi
    for i in $(seq 1 $clipernode); do
        ssh elia@node23 "cd genbyzproto; java -cp \"bin/*:lib/*\" MainClient -c $5 -i $ID -d $1 $dbg -a 2 $tpcc $np $msgs $locality -w $warehouse -r asia >> logs/clients/asia/$ID-cli-ap-south-1.txt" & sleep .05
        ((ID = $ID + 1))
    done
    # cli-ap-southeast-1 (Singapore)
    if [ "$6" -eq 6 ]; then warehouse=2; fi
    for i in $(seq 1 $clipernode); do
        ssh elia@node24 "cd genbyzproto; java -cp \"bin/*:lib/*\" MainClient -c $5 -i $ID -d $1 $dbg -a 2 $tpcc $np $msgs $locality -w $warehouse -r asia >> logs/clients/asia/$ID-cli-ap-southeast-1.txt" & sleep .05
        ((ID = $ID + 1))
    done
    # cli-ap-northeast-1 (Tokyo)
    if [ "$6" -eq 6 ]; then warehouse=5; fi
    for i in $(seq 1 $clipernode); do
        ssh elia@node25 "cd genbyzproto; java -cp \"bin/*:lib/*\" MainClient -c $5 -i $ID -d $1 $dbg -a 2 $tpcc $np $msgs $locality -w $warehouse -r asia >> logs/clients/asia/$ID-cli-ap-northeast-1.txt" & sleep .05
        ((ID = $ID + 1))
    done
    # cli-ap-southeast-2 (Sydney)
    if [ "$6" -eq 6 ]; then warehouse=5; fi
    for i in $(seq 1 $clipernode); do
        ssh elia@node26 "cd genbyzproto; java -cp \"bin/*:lib/*\" MainClient -c $5 -i $ID -d $1 $dbg -a 2 $tpcc $np $msgs $locality -w $warehouse -r asia >> logs/clients/asia/$ID-cli-ap-southeast-2.txt" & sleep .05
        ((ID = $ID + 1))
    done
    echo "started $5 clients" >> ~/genbyzproto/logs/executions.log

    echo "waiting..." >> ~/genbyzproto/logs/executions.log
    while :
    do
        sleep 2;
        nodeFiles=`find ./files -name 'NodeFinished*' | wc -l` #Count files and store in a variable
        if [ "$nodeFiles" -ge $6 ]; then sleep 5; break; fi
    done

    echo "all nodes done" >> ~/genbyzproto/logs/executions.log; ./scripts/kill.sh; echo "processes killed at " $(date)  >> ~/genbyzproto/logs/executions.log; sleep 1;

    if grep -q "true" ~/genbyzproto/files/stop; then echo "found stop, exiting..." >> ~/genbyzproto/logs/executions.log; break; fi
    echo "all queues empty !"  >> ~/genbyzproto/logs/executions.log; sleep 1;

    echo "starting cycle validation ("$(date)")" >> ~/genbyzproto/logs/executions.log; java -Xmx4024m -cp "bin/*:lib/*" util.Validator > ~/genbyzproto/logs/validationresult.txt;
    cat ~/genbyzproto/logs/validationresult.txt >> ~/genbyzproto/logs/executions.log;
    if grep -q "true" ~/genbyzproto/logs/validationresult.txt; then echo "cycle detected!" >> ~/genbyzproto/logs/executions.log; break; fi
    echo "no cycles detected ("$(date)")" >> ~/genbyzproto/logs/executions.log;

done
