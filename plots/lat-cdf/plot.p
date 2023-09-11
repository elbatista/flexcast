
set terminal pdf dashed size 5, 3 font ',20'
set style data histogram
set style fill pattern border -1
set boxwidth 1
set grid ytics
set key right top maxrow 3 font "Helvetica, 24"
set ylabel font "Helvetica,28"
set xtics font "Helvetica, 24"
set ytics font "Helvetica, 22"
set xtics out nomirror

##### CDFS
set yrange [0:1]
set xrange [1:500]

set output 'cdf-dest1-90.pdf'
set ylabel "CDF"
set xlabel "Latency (ms)"
set key right bottom maxrow 3 font "Helvetica, 23"
set xtics out nomirror rotate by 60 right
set xlabel font "Helvetica,28"
#************************************************************
# 99 % LOCALITY
#************************************************************
# DEST 1
set output 'cdf-dest1-99.pdf'

plot \
"CDF_flexcast_99%loc_node1.txt" u 1:(.0000155) smooth cumulative w linespoint pointinterval 50 t "FlexCast", \
"CDF_byzcast_99%loc_node1.txt"      u 1:(.000027) smooth cumulative w linespoint pointinterval 30 t "ByzCast", \
"CDF_skeen_99%loc_node1.txt"    u 1:(.000045) smooth cumulative w linespoint pointinterval 30 t "Skeen"


## DEST 2
#set output 'cdf-dest2-99.pdf'
#
#plot \
#"CDF_flexcast_99%loc_node2.txt" u 1:(.0000067) smooth cumulative w linespoint pointinterval 50 t "FlexCast", \
#"CDF_byzcast_99%loc_node2.txt"      u 1:(.0000058) smooth cumulative w linespoint pointinterval 30 t "ByzCast", \
#"CDF_skeen_99%loc_node2.txt"    u 1:(.0000079) smooth cumulative w linespoint pointinterval 30 t "Skeen"
#
#
### DEST 3
#set output 'cdf-dest3-99.pdf'
#
#plot \
#"CDF_flexcast_99%loc_node3.txt" u 1:(.000675) smooth cumulative w linespoint pointinterval 50 t "FlexCast", \
#"CDF_byzcast_99%loc_node3.txt"      u 1:(.0006) smooth cumulative w linespoint pointinterval 30 t "ByzCast", \
#"CDF_skeen_99%loc_node3.txt"    u 1:(.00079) smooth cumulative w linespoint pointinterval 30 t "Skeen"