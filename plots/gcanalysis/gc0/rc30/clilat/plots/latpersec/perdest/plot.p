set terminal pdf dashed size 5, 2.5 font ",18" 
set style data histogram 
set key right top maxrow 2 
set ylabel "Latency (ms)" 
set xlabel "Time (sec)" 
set grid ytics lt 0 lw 1 
set xtics rotate by 90 right font ',8' nomirror
set output 'experiments/flexcast-reconfig/3nodes/150cli/95%/gc0/rc30/clilat/plots/latpersec/perdest/lat[1,2].pdf' 
plot 'experiments/flexcast-reconfig/3nodes/150cli/95%/gc0/rc30/clilat/plots/latpersec/perdest/lat[1,2].txt' using ($2/1000):xticlabel(1) t "Dests [1,2] " 
set output 'experiments/flexcast-reconfig/3nodes/150cli/95%/gc0/rc30/clilat/plots/latpersec/perdest/lat[0,2].pdf' 
plot 'experiments/flexcast-reconfig/3nodes/150cli/95%/gc0/rc30/clilat/plots/latpersec/perdest/lat[0,2].txt' using ($2/1000):xticlabel(1) t "Dests [0,2] " 
set output 'experiments/flexcast-reconfig/3nodes/150cli/95%/gc0/rc30/clilat/plots/latpersec/perdest/lat[0,1].pdf' 
plot 'experiments/flexcast-reconfig/3nodes/150cli/95%/gc0/rc30/clilat/plots/latpersec/perdest/lat[0,1].txt' using ($2/1000):xticlabel(1) t "Dests [0,1] " 
set output 'experiments/flexcast-reconfig/3nodes/150cli/95%/gc0/rc30/clilat/plots/latpersec/perdest/lat[0,1,2].pdf' 
plot 'experiments/flexcast-reconfig/3nodes/150cli/95%/gc0/rc30/clilat/plots/latpersec/perdest/lat[0,1,2].txt' using ($2/1000):xticlabel(1) t "Dests [0,1,2] " 
set output 'experiments/flexcast-reconfig/3nodes/150cli/95%/gc0/rc30/clilat/plots/latpersec/perdest/lat[1,2,0].pdf' 
plot 'experiments/flexcast-reconfig/3nodes/150cli/95%/gc0/rc30/clilat/plots/latpersec/perdest/lat[1,2,0].txt' using ($2/1000):xticlabel(1) t "Dests [1,2,0] " 
set output 'experiments/flexcast-reconfig/3nodes/150cli/95%/gc0/rc30/clilat/plots/latpersec/perdest/lat[2,0].pdf' 
plot 'experiments/flexcast-reconfig/3nodes/150cli/95%/gc0/rc30/clilat/plots/latpersec/perdest/lat[2,0].txt' using ($2/1000):xticlabel(1) t "Dests [2,0] " 
set output 'experiments/flexcast-reconfig/3nodes/150cli/95%/gc0/rc30/clilat/plots/latpersec/perdest/lat[1,0].pdf' 
plot 'experiments/flexcast-reconfig/3nodes/150cli/95%/gc0/rc30/clilat/plots/latpersec/perdest/lat[1,0].txt' using ($2/1000):xticlabel(1) t "Dests [1,0] " 
