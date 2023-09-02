for i in $(seq 1 $1)
do
    ssh -o StrictHostKeyChecking=accept-new node$i "pkill -f 'java.*Main*'"
done
echo killed all processes