#!/bin/bash
if [ "$#" -lt 1 ]; then 
    #echo "Usage: $0 <duration:sec> <debug:bool> <skeen:bool> <tpcc:bool> <#clis> <#servers> <latency:ms> <#experiments> <#partitions> <pfon:bool> <cpu:bool> <#msgs> <batch:bool> <batchtimeout:nanos> <%locality> <#clispernode>"; 
    echo  "Usage: $0 <#nodes>"
    exit 0; 
fi

basedir=/usr/local/projects/flexcast;
rm -f $basedir/wan/ifaces.csv;
for i in $(seq 1 $(($1)))
do
    echo "node$i,10.10.1."$(($i+1))",enp130s0f0" >> $basedir/wan/ifaces.csv;
    iniport=$(($iniport+10));
done
