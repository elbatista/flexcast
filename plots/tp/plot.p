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


plot \
"TP_12nodes_99%_gc0.txt" using 1:($2/1000):xtic(1) title "Skeen" with linespoints lc -1, \
"TP_12nodes_99%_gc0.txt" using 1:($3/1000):xtic(1) title "ByzCast" with linespoints lc -1 pt 6, \
"TP_12nodes_99%_gc0.txt" using 1:($4/1000):xtic(1) title "FlexCast" with linespoints lc -1 pt 5

