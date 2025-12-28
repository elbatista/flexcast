set terminal pdf dashed size 5, 2.5 font ",18" 
set key right top maxrow 2 
set ylabel "TP (ops/sec)" 
set xlabel "Time (sec)" 
set grid ytics lt 0 lw 1 
set grid xtics lt 0 lw 1 

set output 'tp.pdf' 
plot 'TP_Reconf.txt' using 1 t "TP" with lines 
