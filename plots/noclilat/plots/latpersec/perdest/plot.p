set terminal pdf dashed size 5, 2.5 font ",18" 
set key right top maxrow 2 
set ylabel "Latency (ms)" 
set xlabel "Time (sec)" 
set grid ytics lt 0 lw 1 

set yrange[0:1000]

set output 'lat[0,1].pdf' 
plot 'lat[0,1].txt' using ($2/1000) t "[0,1] "  w lines, \
     'lat[1,0].txt' using ($2/1000) notitle w lines

set output 'lat[0,2].pdf' 
plot 'lat[0,2].txt' using ($2/1000) t "[0,2] "  w lines, \
     'lat[2,0].txt' using ($2/1000) notitle w lines

set output 'lat[1,2].pdf' 
plot 'lat[1,2].txt' using ($2/1000) t "[1,2] "  w lines


set output 'lat[0,1,2].pdf' 
plot 'lat[0,1,2].txt' using ($2/1000) t "[0,1,2] "  w lines, \
     'lat[1,2,0].txt' using ($2/1000) notitle w lines

