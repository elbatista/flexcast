#!/bin/bash
duration=60;
algo=(0 1 2);
clients=(96 192 384);
#12 24 48     768
servers=12;
nodes=24;
locality=99;
# msgs=0;
gc=0;
clispernode=1;
algodesc=("flexcast" "skeen" "byzcast")

for a in "${algo[@]}"
do
    rm ./config/*.conf*
    cat ./config/${servers}nodes/servers-${algodesc[$a]}.conf > ./config/servers.conf
    cat ./config/${servers}nodes/clients.conf > ./config/clients.conf
    if [ "$a" -eq 2 ]; then cat ./config/${servers}nodes/byzcast-tree.config > ./config/byzcast.config; fi
    if [ "$a" -eq 0 ]; then 
        gc=1000; 
    else 
        gc=0; 
    fi
    for c in "${clients[@]}"
    do
        clispernode=$(($c/12))
        ./scripts/runCluster.sh $duration $a $c $servers $nodes $locality 0 $gc $clispernode;
        # cat ./config/servers.conf
    done
done
