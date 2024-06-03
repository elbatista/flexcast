#!/bin/bash
if [ "$#" -lt 5 ]; then 
    echo  "Usage: $0 <nodesfrom> <nodesto> <netip> <basedir> <iscloudlab>"
    exit 0; 
fi

basedir=$4;
rm -f $basedir/wan/ifaces.csv;
for i in $(seq $(($1)) $(($2)))
do
    iface=$(ssh node$i "ip route list $3.0/24" | awk '{print $3}')
    if [ "$5" == "true" ]; then 
        echo "node$i,$3."$(($i+1))",$iface" >> $basedir/wan/ifaces.csv;
    else
        echo "node$i,$3."$(($i))",$iface" >> $basedir/wan/ifaces.csv;
    fi
    iniport=$(($iniport+10));
done
