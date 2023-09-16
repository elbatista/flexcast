set terminal pdf #dashed size 5, 3 font "Helvetica, 22"
set output 'multi-skeen.pdf'

set multiplot layout 3, 1 
set tmargin 2
set style data histogram
#set style fill pattern border -1
set boxwidth 1
set grid ytics
set ytics font "Helvetica, 13"
unset xtics
unset key

set yrange [0:2000]
set ytics (500,1500,2000)
set title "#Messages"
plot "skeen_12nodes_192cli_99%_gc0-aws-loc-file-90%___v2.txt" using (($2)):xtic(1)

#
set title "Avg Message Size (Bytes)"
unset key
set yrange [0:600]
set ytics (100,300,500)
plot "skeen_12nodes_192cli_99%_gc0-aws-loc-file-90%___v2.txt" using (($3)):xtic(1)

#
set title "Volume (KB)"
set xlabel "Node" font "Helvetica, 16"
set xtics out nomirror
set yrange [0:300]
set ytics (100,200,300)
plot "skeen_12nodes_192cli_99%_gc0-aws-loc-file-90%___v2.txt" using (($2*$3/1024)):xtic(1)

#
unset multiplot
unset xlabel