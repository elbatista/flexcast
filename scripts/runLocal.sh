if [ "$#" -lt 6 ]; then echo "Usage: $0 <duration:sec> <debug:bool> <algo:0-flex;1-skeen;2-byz> <tpcc:bool> <#clis> <#servers> <#partitions> <profileron:bool> <#numMsgs> <batchSize> <locality>"; exit 0; fi
i=0; dbg=""; tpcc=""; np=""; p=""; nmsgs=""; bs="";locality=""; count=0; ant clean; ant; rm -f -r logs/*  files/*;
if [ "$2" == "true" ]; then dbg="-dbg"; fi
if [ "$4" == "true" ]; then tpcc="-t"; fi
if [ "$#" -gt 6 ]; then np="-np $7"; fi
if [ "$#" -gt 7 ]; then if [ "$8" == "true" ]; then p="-p"; fi fi
if [ "$#" -gt 8 ]; then nmsgs="-m $9"; fi
if [ "$#" -gt 9 ]; then bs="-bs ${10}"; fi
if [ "$#" -gt ${10} ]; then locality="-l ${11}"; fi

pkill -f 'java.*Main*'; sleep 2;

while :
do
    rm -f -r logs/*.txt  files/*; pkill -f 'java.*Main*' ; echo false > files/stop; ((count = count+1));
    echo "------------------------------------------------------------------------------------------------" >> logs/executions.log
    echo "execution $count at" $(date) >> logs/executions.log
    echo $0 $1 $2 $3 $4 $5 $6 $7 $8 $np $9 $bs $locality >> logs/executions.log
    echo "------------------------------------------------------------------------------------------------" >> logs/executions.log

    # Start servers
    ((START = $6-1))
    for ((i = START; i >= 0; i-=1)) ; do
        java -cp "bin/*:lib/*" MainServer -i $i -a $3 -d $1  -c $5 >> logs/node$i.txt & sleep .05
    done
    echo started $6 servers >> logs/executions.log

    # Start clients
    ((END = $5-1))
    # for j in $(seq 0 $END); do
        java -cp "bin/*:lib/*" MainClient -c $5 -i 0 -d $1 -a $3 $tpcc  $locality -w 0 >> logs/cli0.txt &
        # java -cp "bin/*:lib/*" MainClient -c $5 -i 1 -d $1 $f $dbg -a $3 $tpcc $np $nmsgs $locality -w 1 >> logs/cli1.txt &
        # java -cp "bin/*:lib/*" MainClient -c $5 -i 2 -d $1 $f $dbg -a $3 $tpcc $np $nmsgs $locality -w 2 >> logs/cli2.txt &
        # java -cp "bin/*:lib/*" MainClient -c $5 -i 3 -d $1 $f $dbg -a $3 $tpcc $np $nmsgs $locality -w 3 >> logs/cli3.txt &
        # java -cp "bin/*:lib/*" MainClient -c $5 -i 4 -d $1 $f $dbg -a $3 $tpcc $np $nmsgs $locality -w 4 >> logs/cli4.txt &
        # java -cp "bin/*:lib/*" MainClient -c $5 -i 5 -d $1 $f $dbg -a $3 $tpcc $np $nmsgs $locality -w 5 >> logs/cli5.txt &

        # java -cp "bin/*:lib/*" MainClient -c $5 -i 6 -d $1 $f $dbg -a $3 $tpcc $np $nmsgs $locality -w 0 >> logs/cli6.txt &
        # java -cp "bin/*:lib/*" MainClient -c $5 -i 7 -d $1 $f $dbg -a $3 $tpcc $np $nmsgs $locality -w 1 >> logs/cli7.txt &
        # java -cp "bin/*:lib/*" MainClient -c $5 -i 8 -d $1 $f $dbg -a $3 $tpcc $np $nmsgs $locality -w 2 >> logs/cli8.txt &
        # java -cp "bin/*:lib/*" MainClient -c $5 -i 9 -d $1 $f $dbg -a $3 $tpcc $np $nmsgs $locality -w 3 >> logs/cli9.txt &
        # java -cp "bin/*:lib/*" MainClient -c $5 -i 10 -d $1 $f $dbg -a $3 $tpcc $np $nmsgs $locality -w 4 >> logs/cli10.txt &
        # java -cp "bin/*:lib/*" MainClient -c $5 -i 11 -d $1 $f $dbg -a $3 $tpcc $np $nmsgs $locality -w 5 >> logs/cli11.txt &
    # done
    # echo started $5 clients >> logs/executions.log
    
    echo "waiting..."  >> logs/executions.log;
    while :
    do
        sleep 1;
        nodeFiles=`find ./files -name 'NodeFinished*' | wc -l` #Count files and store in a variable
        if [ "$nodeFiles" -ge $6 ]; then sleep 1; break; fi
    done
    echo "all nodes done"  >> logs/executions.log; pkill -f 'java.*Main*'; echo "processes killed"  >> logs/executions.log

    # se teve fila nao vazia, para experimentos
    if grep -q "true" files/stop; then echo "found stop, exiting..." >> logs/executions.log; exit 0; fi

    # se teve ciclos, para experimentos
    echo "starting cycle validation ("$(date)")" >> logs/executions.log; java -cp "bin/*:lib/*" util.Validator > logs/validationresult.txt
    if grep -q "true" logs/validationresult.txt; then echo "cycle detected!" >> logs/executions.log; cat logs/validationresult.txt; exit 0; fi
    echo "no cycles detected ("$(date)")" >> logs/executions.log; 
    
    exit 0; # <- para executar somente uma vez
done