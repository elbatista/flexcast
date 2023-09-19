set terminal pdf dashed size 5, 3 font ',20'
set style data histogram
set style fill pattern border -1
set boxwidth 1
set grid ytics
set xlabel "Node"
set key right top maxrow 2 font "Helvetica, 22"
set ylabel font "Helvetica,28"
set xtics font "Helvetica, 24"
set ytics font "Helvetica, 22"
set xtics out nomirror
set output 'msgpernode-ttlog.pdf'
set yrange[0:15]
set ylabel "#Messages (x1000)"

plot "flexcast_12nodes_240cli_99%_gc0_ttlog.txt" using ($2/1000):xtic(1) title "FlexCast"
plot "skeen_12nodes_240cli_99%_gc0_ttlog.txt"    using ($2/1000):xtic(1) title "Skeen"
plot "byzcast_12nodes_240cli_99%_gc0_ttlog.txt"  using ($2/1000):xtic(1) title "ByzCast"
