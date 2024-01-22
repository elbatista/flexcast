#!/bin/bash
if [ "$#" -lt 1 ]; then 
    echo  "Usage: $0 <#nodes>"
    exit 0; 
fi

basedir=~/flexcast;
rm -f $basedir/wan/ifaces.csv;
for i in $(seq 1 $(($1)))
do
    iface=$(ssh node$i "ip route list 192.168.3.0/24" | awk '{print $3}')
    echo "node$i,192.168.3.$i,$iface" >> $basedir/wan/ifaces.csv;
    iniport=$(($iniport+10));
done
