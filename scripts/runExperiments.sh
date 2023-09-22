#!/bin/bash
duration=60;
algo=(2);
clients=(192);
# 24 48 96 192 384 768
# 24 960
servers=12;
nodes=36;
locality=90;
msgs=0;
gc=0;
clispernode=1;
algodesc=("flexcast" "skeen" "byzcast");
tpcc='null';
payload='null';         #'-payload';
thinktime='null';       #'-tt';
localm='null';
dag_tree=3;

for a in "${algo[@]}"
do
    rm ./config/*.conf*
    cat ./config/${servers}nodes/servers-${algodesc[$a]}.conf > ./config/servers.conf
    cat ./config/${servers}nodes/clients.conf > ./config/clients.conf
    cat ./config/${servers}nodes/locality-${algodesc[$a]}.conf > ./config/locality.conf
    if [ "$a" -eq 2 ]; then cat ./config/${servers}nodes/byzcast-tree.config > ./config/byzcast.config; fi
    if [ "$a" -eq 0 ]; then 
        gc=0; 
    else 
        gc=0; 
    fi
    for c in "${clients[@]}"
    do
        clispernode=$(($c/24))
        ./scripts/runCluster.sh $duration $a $c $servers $nodes $locality $msgs $gc $clispernode $tpcc $payload $thinktime $localm $dag_tree;
        # cat ./config/servers.conf
    done
done
