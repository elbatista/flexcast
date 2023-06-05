if [ "$#" -lt 7 ]; then echo "Usage: $0 <duration:sec> <algo:0-flex;1-skeen;2-byz> <#clis> <#servers> <locality> <#msgs> <#exp>"; exit 0; fi
i=0; tpcc=""; locality=""; exe=0; ant clean; ant; rm -f -r logs/*  files/*;
duration=$1; algo=$2; clis=$3; servers=$4; locality=$5; msgs=$6; pkill -f 'java.*Main*'; sleep 1;
rm -f -r logs/*.txt  files/*; pkill -f 'java.*Main*' ; echo false > files/stop; 
log="";
for exe in $(seq 1 $7); do

rm -f -r logs/*.txt files/* results/*; echo false > files/stop;
echo "------------------------------------------------------------------------------------------------" >> logs/executions.log
echo "execution $exe at" $(date) >> logs/executions.log
echo $0 duration $duration algo $algo clis $clis servers $servers locality $locality msgs $msgs >> logs/executions.log
echo "------------------------------------------------------------------------------------------------" >> logs/executions.log

# Start servers
((START = $servers-1))
for ((i = START; i >= 0; i-=1)) ; do
    java -cp "bin/*:lib/*" MainServer -i $i -a $algo -d $duration -c $clis $log >> logs/node$i.txt & sleep .05
done
echo started $servers servers >> logs/executions.log

# Start clients
((END = $clis-1))
warehouse=0
for j in $(seq 0 $END); do
    if [ $warehouse -eq $servers ]; then warehouse=0; fi
    java -cp "bin/*:lib/*" MainClient -c $clis -i $j -d $duration -a $algo -l $locality -w $warehouse -m $msgs $log >> logs/cli$j.txt &
    ((warehouse=$warehouse+1))
done
echo started $clis clients >> logs/executions.log


echo "waiting..."  >> logs/executions.log;
while :
do
    sleep 1;
    nodeFiles=`find ./files -name 'NodeFinished*' | wc -l` #Count files and store in a variable
    if [ "$nodeFiles" -ge $servers ]; then sleep 1; break; fi
done
echo "all nodes done"  >> logs/executions.log; pkill -f 'java.*Main*'; echo "processes killed"  >> logs/executions.log

# se teve fila nao vazia, para experimentos
if grep -q "true" files/stop; then echo "found stop, exiting..." >> logs/executions.log; exit 0; fi

# se teve ciclos, para experimentos
# echo "starting cycle validation ("$(date)")" >> logs/executions.log; java -cp "bin/*:lib/*" util.Validator > logs/validationresult.txt
# if grep -q "true" logs/validationresult.txt; then echo "cycle detected!" >> logs/executions.log; cat logs/validationresult.txt; exit 0; fi
# echo "no cycles detected ("$(date)")" >> logs/executions.log; 
# cat logs/validationresult.txt >> logs/executions.log; 
done