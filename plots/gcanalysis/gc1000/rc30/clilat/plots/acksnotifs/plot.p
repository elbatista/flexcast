set terminal pdf dashed size 5, 2.5 font ",18" 
set key left top maxrow 2 
set ylabel "Msgs/sec" 
set xlabel "Time (sec)" 
set grid ytics lt 0 lw 1 
set grid xtics lt 0 lw 1 
set xrange[0:120]
set xtics rotate by 50 right
set output 'experiments/flexcast-reconfig/3nodes/150cli/95%/gc1000/rc30/clilat/plots/acksnotifs/acksnotifs.pdf' 
plot 'experiments/flexcast-reconfig/3nodes/150cli/95%/gc1000/rc30/clilat/plots/acksnotifs/data.txt' using ($2):xtic($1) t "Acks" w lines, \
     'experiments/flexcast-reconfig/3nodes/150cli/95%/gc1000/rc30/clilat/plots/acksnotifs/data.txt' using ($3):xtic($1) t "Notifs" w lines
set ylabel "Graph Size" 
set output 'experiments/flexcast-reconfig/3nodes/150cli/95%/gc1000/rc30/clilat/plots/acksnotifs/graphsizes.pdf' 
plot 'experiments/flexcast-reconfig/3nodes/150cli/95%/gc1000/rc30/clilat/files/node0-acksNotifs.txt' using ($4):xtic($1) t "Node0" w lines , \
     'experiments/flexcast-reconfig/3nodes/150cli/95%/gc1000/rc30/clilat/files/node1-acksNotifs.txt' using ($4):xtic($1) t "Node1" w lines , \
     'experiments/flexcast-reconfig/3nodes/150cli/95%/gc1000/rc30/clilat/files/node2-acksNotifs.txt' using ($4):xtic($1) t "Node2" w lines 
set ylabel "Volume - Bytes" 
set yrange[0:600000]
set output 'experiments/flexcast-reconfig/3nodes/150cli/95%/gc1000/rc30/clilat/plots/acksnotifs/volume.pdf' 
plot 'experiments/flexcast-reconfig/3nodes/150cli/95%/gc1000/rc30/clilat/files/node0-acksNotifs.txt' using ($5):xtic($1) t "Node0" w lines , \
     'experiments/flexcast-reconfig/3nodes/150cli/95%/gc1000/rc30/clilat/files/node1-acksNotifs.txt' using ($5):xtic($1) t "Node1" w lines , \
     'experiments/flexcast-reconfig/3nodes/150cli/95%/gc1000/rc30/clilat/files/node2-acksNotifs.txt' using ($5):xtic($1) t "Node2" w lines 
