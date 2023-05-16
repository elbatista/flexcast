# mkdir files
rm -r logs files results
mkdir logs files results
scp -r elia@node90:genbyzproto/results/* ./results/ 
scp -r elia@node90:genbyzproto/logs/* ./logs/ 
scp -r elia@node90:genbyzproto/files/* ./files/ 
