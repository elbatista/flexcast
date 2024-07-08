if [ "$#" -lt 4 ]; then 
    echo "Usage: $0 <#servers> <#nodes> <locality> <#clis>"
    exit 0;
fi
clis=$4
./scripts/runDSLabCluster.sh 60 0 $clis $1 $2 $3 0 1000 $(($clis/$1)) false true true false 1
