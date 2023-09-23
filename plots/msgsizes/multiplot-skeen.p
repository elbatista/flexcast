set terminal pdf #dashed size 5, 3 font "Helvetica, 22"
set output 'multi-skeen.pdf'

set multiplot layout 3, 1 
set tmargin 2
set style data histogram
set style fill pattern border 2
set boxwidth 1
set grid ytics
set ytics font "Helvetica, 13"
unset xtics
set key top right

set yrange [0:850]
set ytics (0,200,400,600,800)
set title "#Messages / sec"
plot "skeen_12nodes_720cli_99%_gc0-ttlog.txt" using (($2)):xtic(1) t "Skeen"

#
set title "Avg Message Size (Bytes / sec)"
unset key
set yrange [0:210]
set ytics (0, 100,200)
plot "skeen_12nodes_720cli_99%_gc0-ttlog.txt" using (($3)):xtic(1)

#
set title "Volume (KB / sec)"
set xlabel "Node" font "Helvetica, 16"
set xtics out nomirror
set yrange [0:100]
set ytics (0, 50,100)
plot "skeen_12nodes_720cli_99%_gc0-ttlog.txt" using (($2*$3/1024)):xtic(1)

#
unset multiplot
unset xlabel