# rm -r -f logs files results
# mkdir files
# mkdir results
# mkdir logs
# scp -r elia@node90:genbyzproto/results/* ./results/ 
# scp -r elia@node90:genbyzproto/logs/* ./logs/ 
# scp -r elia@node90:genbyzproto/files/* ./files/ 


for i in $(seq 1 $1)
do
    scp -r -o StrictHostKeyChecking=accept-new node$i:/usr/batista/flexcast/results/* ./results/
    scp -r -o StrictHostKeyChecking=accept-new node$i:/usr/batista/flexcast/logs/* ./logs/
    scp -r -o StrictHostKeyChecking=accept-new node$i:/usr/batista/flexcast/files/* ./files/
done