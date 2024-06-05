set terminal pdf dashed size 5, 2.5 font ",18" 
set key right top maxrow 2 
set ylabel "TP (ops/sec)" 
set xlabel "Time (sec)" 
set grid ytics lt 0 lw 1 
set grid xtics lt 0 lw 1 
set output 'experiments/flexcast-reconfig/3nodes/150cli/95%/gc10000/rc30/clilat/plots/tp/tp.pdf' 
plot 'experiments/flexcast-reconfig/3nodes/150cli/95%/gc10000/rc30/clilat/plots/tp/TP_Reconf.txt' using 1 t "TP" with lines 
