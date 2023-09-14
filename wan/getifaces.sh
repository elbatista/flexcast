#!/bin/bash
if [ "$#" -lt 1 ]; then 
    echo  "Usage: $0 <#nodes>"
    exit 0; 
fi

basedir=~/flexcast;
rm -f $basedir/wan/ifaces.csv;
for i in $(seq 1 $(($1)))
do
    iface=$(ssh node$i "ip route list 10.10.1.0/24" | awk '{print $3}')
    echo "node$i,10.10.1."$(($i+1))",$iface" >> $basedir/wan/ifaces.csv;
    iniport=$(($iniport+10));
done
