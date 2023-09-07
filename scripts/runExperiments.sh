#!/bin/bash
duration=60;
algo=(0);
clients=(768);
#12 24 48 96 192 384
servers=12;
nodes=24;
locality=99;
# msgs=0;
gc=0;
clispernode=1;

for a in "${algo[@]}"
do
    if [ "$a" -eq 0 ]; then gc=10000; else gc=0; fi
    for c in "${clients[@]}"
    do
        clispernode=$(($c/12))
        ./scripts/runCluster.sh $duration $a $c $servers $nodes $locality 0 $gc $clispernode;
    done
done
