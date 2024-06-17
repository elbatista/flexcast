set terminal pdf dashed size 5, 2.5 font ",18" 
set key right top maxrow 2 
set ylabel "Latency (ms)" 
set xlabel "Time (sec)" 
set grid ytics lt 0 lw 1 
set grid xtics lt 0 lw 1 
set yrange[0:400]


set output 'lat[1,2].pdf' 
plot 'lat[1,2].txt' using ($2/1000) t "[1,2] " with lines 

set output 'lat[0,2].pdf' 
plot 'lat[0,2].txt' using ($2/1000) t "[0,2] " with lines 

set output 'lat[0,1].pdf' 
plot 'lat[0,1].txt' using ($2/1000) t "[0,1] " with lines 

set output 'lat[0,1,2].pdf' 
plot 'lat[0,1,2].txt' using ($2/1000) t "[0,1,2] " with lines 
