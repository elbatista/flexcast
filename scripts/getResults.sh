rm -r -f logs files results
mkdir files
mkdir results
mkdir logs
scp -r elia@node90:genbyzproto/results/* ./results/ 
scp -r elia@node90:genbyzproto/logs/* ./logs/ 
scp -r elia@node90:genbyzproto/files/* ./files/ 
