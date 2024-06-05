set terminal pdf dashed size 5, 2.5 font ",18" 
set key right top maxrow 2 
set ylabel "Latency (ms)" 
set xlabel "Time (sec)" 
set grid ytics lt 0 lw 1 
set grid xtics lt 0 lw 1 

set yrange[0:400]

set output 'lat.pdf' 
plot 'lat.txt' using ($2/1000) t "Final Latency" with lines
