set terminal pdf dashed size 5, 3 font ',20'
set style data histogram
set style fill pattern border -1
set boxwidth 1
set grid ytics
set ylabel "Volume"
set xlabel "Node"
set key left top maxrow 2 font "Helvetica, 24"
set ylabel font "Helvetica,28"
set xtics font "Helvetica, 24"
set ytics font "Helvetica, 22"
set xtics out nomirror
set output 'msgsizes.pdf'

plot \
"flexcast_12nodes_192cli_99%_gc0-aws-loc-file-90%.txt" using (($2*$3)*.0001):xtic(1) title "FlexCast", \
"skeen_12nodes_192cli_99%_gc0-aws-loc-file-90%.txt" using (($2*$3)*.0001):xtic(1) title "Skeen", \
"byzcast_12nodes_192cli_99%_gc0-aws-loc-file-90%.txt" using (($2*$3)*.0001):xtic(1) title "ByzCast"
