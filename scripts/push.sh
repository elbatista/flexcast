
# scp -r ./config elia@node1:genbyzproto
# scp -r ./scripts elia@node1:genbyzproto
ant clean
ant

ssh elia@node90 "rm -r ~/genbyzproto/bin/*"
scp -r ./bin elia@node90:genbyzproto
