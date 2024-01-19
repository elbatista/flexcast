if [ "$#" -lt 9 ]; then 
    echo "Usage: $0 <duration:sec> <algo:0-flex;1-skeen;2-byz> <#clis> <#servers> <%locality> <#msgs> <#exp> <#gc(ms)> <gap>"; 
    exit 0; 
fi

pkill -f 'java.*Main*'; 
ant clean; 
ant; 
i=0;
exe=0; 
log="";
duration=$1; 
algo=$2; 
clis=$3; 
servers=$4; 
locality=$5; 
msgs=$6; 
numExperiments=$7;
gc=$8; 
gap=$9; 

rm -f -r logs/* files/*; 
echo false > files/stop; 

for exe in $(seq 1 $numExperiments); do

    rm -f -r logs/*.txt files/* results/*; echo false > files/stop;
    echo "------------------------------------------------------------------------------------------------" >> logs/executions.log
    echo "execution $exe (of $numExperiments) at" $(date) >> logs/executions.log
    echo $0 duration: $duration\; algo: $algo\; clis: $clis\; servers: $servers\; locality: $locality%\; msgs: $msgs\; gc: $gc\(ms\)\; >> logs/executions.log
    echo "------------------------------------------------------------------------------------------------" >> logs/executions.log
    
    totalClis=$clis
    # one more client for the gc:
    if [ "$gc" -gt 0 ]; then 
        ((totalClis = $clis+1))
    fi

    # one more gap client for each server:
    if [ "$gap" -gt 0 ]; then 
        ((totalClis = $totalClis+10))
    fi

    # Start servers
    ((START = $servers-1))
    for ((i = START; i >= 0; i-=1)) ; do
        java -cp "bin/*:lib/*" MainServer -i $i -a $algo -d $duration -c $totalClis $log >> logs/node$i.txt & sleep .05
    done
    echo started $servers servers >> logs/executions.log

    # Start clients
    ((END = $clis-1))
    warehouse=0
    for j in $(seq 0 $END); do
        if [ $warehouse -eq $servers ]; then warehouse=0; fi
        java -cp "bin/*:lib/*" MainClient -c $totalClis -i $j -d $duration -a $algo -l $locality -w $warehouse -m $msgs $log >> logs/cli$j.txt &
        ((warehouse=$warehouse+1))
    done
    echo started $clis clients >> logs/executions.log

    if [ "$gc" -gt 0 ]; then 
        java -cp "bin/*:lib/*" MainClient -c $totalClis -i $clis -d $duration -a $algo $log -gc $gc >> logs/gc_client.txt &
        echo started gc client >> logs/executions.log
    fi

    if [ "$gap" -gt 0 ]; then 
        # ((START2 = $servers-1))
        ((gapcliid = $clis+1))
        for ((i = 0; i < 10; i+=1)) ; do
            # java -cp "bin/*:lib/*" MainServer -i $i -a $algo -d $duration -c $totalClis $log >> logs/node$i.txt & sleep .05
            java -cp "bin/*:lib/*" MainClient -gapcli -c $totalClis -i $gapcliid -d $duration -a $algo -l $locality -w $i -m $msgs $log >> logs/gapcli$gapcliid.txt &
            ((gapcliid=$gapcliid+1))
        done
        echo started gap clients >> logs/executions.log
    fi

    echo "waiting..."  >> logs/executions.log;
    while :
    do
        sleep 1;
        nodeFiles=`find ./files -name 'NodeFinished*' | wc -l` #Count files and store in a variable
        if [ "$nodeFiles" -ge $servers ]; then sleep 1; break; fi
        if grep -q "true" files/stop; then echo "found stop, exiting..." >> logs/executions.log; exit 0; fi
    done
    echo "all nodes done"  >> logs/executions.log; 
    pkill -f 'java.*Main*'; 
    echo "processes killed"  >> logs/executions.log


    # exit 0;

    # se teve ciclos, para experimentos
    echo "starting cycle validation ("$(date)")" >> logs/executions.log; java -cp "bin/*:lib/*" util.Validator > logs/validationresult.txt
    if grep -q "true" logs/validationresult.txt; then echo "cycle detected!" >> logs/executions.log; cat logs/validationresult.txt; exit 0; fi
    echo "no cycles detected ("$(date)")" >> logs/executions.log; 
    cat logs/validationresult.txt >> logs/executions.log; 
done