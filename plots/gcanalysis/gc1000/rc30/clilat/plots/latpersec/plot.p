set terminal pdf dashed size 5, 2.5 font ",18" 
set key right top maxrow 2 
set ylabel "Latency (ms)" 
set xlabel "Time (sec)" 
set grid ytics lt 0 lw 1 
set grid xtics lt 0 lw 1 
set xrange[0:120]
set xtics rotate by 50 right
set output 'experiments/flexcast-reconfig/3nodes/150cli/95%/gc1000/rc30/clilat/plots/latpersec/lat.pdf' 
plot 'experiments/flexcast-reconfig/3nodes/150cli/95%/gc1000/rc30/clilat/plots/latpersec/lat.txt' using ($2/1000):xtic($1) t "Final Latency" w lines 
