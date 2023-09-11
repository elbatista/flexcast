set key left top maxrow 6 font "Helvetica, 12"
set xlabel "# Clients"
set ylabel "Throughput (kops/sec)"
set grid ytics lt 0 lw 1
set terminal pdf dashed size 5, 3 font ',20'
set xlabel font "Helvetica, 22"
set ylabel font "Helvetica, 22"
set xtics font "Helvetica, 15"
set ytics font "Helvetica, 22"
set output 'tp.pdf'
set xtics out nomirror rotate by 60 right
#set yrange [0:6]

plot \
"tp.txt" using 1:($2/1000):xtic(1) title "Skeen" with linespoints lc -1, \
"tp.txt" using 1:($3/1000):xtic(1) title "ByzCast" with linespoints lc -1 pt 6, \
"tp.txt" using 1:($4/1000):xtic(1) title "FlexCast" with linespoints lc -1 pt 5
#, \
#"tp.txt" using 1:($4/1000):xtic(1) title "FlexCast 100" with linespoints lc -1 pt 2, \
#"tp.txt" using 1:($5/1000):xtic(1) title "FlexCast 1000" with linespoints lc -1 pt 4, \
#"tp.txt" using 1:($7/1000):xtic(1) title "FlexCast 3000" with linespoints lc -1 pt 7, \
#"tp.txt" using 1:($8/1000):xtic(1) title "FlexCast 5000" with linespoints lc -1 pt 8