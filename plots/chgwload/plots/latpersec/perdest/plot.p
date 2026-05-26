set terminal pdf dashed size 5, 2.5 font ",18" 

set key right top maxrow 2 
set ylabel "Latency (ms)" 
set xlabel "Time (sec)" 
set grid ytics lt 0 lw 1 
set xtics rotate by 90 right font ',8' nomirror

set output 'lat[0,1,2].pdf' 

plot 'lat[0,1,2].txt' using ($2/1000):xticlabel(1) w lines t "Dests [0,1,2] ", \
     'lat[0,2,1].txt' using ($2/1000):xticlabel(1) w lines notitle, \
     'lat[1,0,2].txt' using ($2/1000):xticlabel(1) w lines notitle, \
     'lat[1,2,0].txt' using ($2/1000):xticlabel(1) w lines notitle

set output 'lat[0,2].pdf' 
plot 'lat[0,2].txt' using ($2/1000):xticlabel(1) w lines t "Dests [0,2] ", \
     'lat[2,0].txt' using ($2/1000):xticlabel(1) w lines notitle

set output 'lat[0,1].pdf' 
plot 'lat[0,1].txt' using ($2/1000):xticlabel(1) w lines t "Dests [0,1] ", \
     'lat[1,0].txt' using ($2/1000):xticlabel(1) w lines notitle

set output 'lat[1,2].pdf' 
plot 'lat[1,2].txt' using ($2/1000):xticlabel(1) w lines t "Dests [1,2] ", \
     'lat[2,1].txt' using ($2/1000):xticlabel(1) w lines notitle

#set output 'lat[0,2].pdf' 
#plot 'lat[0,2].txt' using ($2/1000):xticlabel(1) t "Dests [0,2] " 
#set output 'lat[0,1].pdf' 
#plot 'lat[0,1].txt' using ($2/1000):xticlabel(1) t "Dests [0,1] " 
#set output 'lat[0,1,2].pdf' 
#plot 'lat[0,1,2].txt' using ($2/1000):xticlabel(1) t "Dests [0,1,2] " 
#set output 'lat[2,1].pdf' 
#plot 'lat[2,1].txt' using ($2/1000):xticlabel(1) t "Dests [2,1] " 
#set output 'lat[0,2,1].pdf' 
#plot 'lat[0,2,1].txt' using ($2/1000):xticlabel(1) t "Dests [0,2,1] " 
#set output 'lat[1,2,0].pdf' 
#plot 'lat[1,2,0].txt' using ($2/1000):xticlabel(1) t "Dests [1,2,0] " 
#set output 'lat[2,0].pdf' 
#plot 'lat[2,0].txt' using ($2/1000):xticlabel(1) t "Dests [2,0] " 
#set output 'lat[1,0].pdf' 
#plot 'lat[1,0].txt' using ($2/1000):xticlabel(1) t "Dests [1,0] " 
#set output 'lat[1,0,2].pdf' 
#plot 'lat[1,0,2].txt' using ($2/1000):xticlabel(1) t "Dests [1,0,2] " 
