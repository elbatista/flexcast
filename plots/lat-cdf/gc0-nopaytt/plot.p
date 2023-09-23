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
set ylabel "CDF"
set xlabel "Latency (ms)"
set key right bottom maxrow 3 font "Helvetica, 23"
set xtics out nomirror rotate by 60 right
set xlabel font "Helvetica,28"
#************************************************************
# 99 % LOCALITY
#************************************************************
# DEST 1

##### CDFS
#set yrange [0:1]
set xrange [1:500]
set output 'cdf-dest1-99.pdf'

plot \
"CDF_flexcast_99%loc_node1.txt" using 1:(.000017) smooth cumul w lines t "FlexCast", \
"CDF_byzcast_99%loc_node1.txt"  using 1:(.0000099) smooth cumul w lines t "ByzCast", \
"CDF_skeen_99%loc_node1.txt"    using 1:(.000013) smooth cumul w lines t "Skeen"

#"CDF_flexcast_99%loc_node1.txt" using 1:(.001) smooth cumul w lines t "FlexCast", \
#"CDF_byzcast_99%loc_node1.txt"  using 1:(.001) smooth cumul w lines t "ByzCast", \
#"CDF_skeen_99%loc_node1.txt"    using 1:(.001) smooth cumul w lines t "Skeen"


#set xrange [1:1500]
#
## DEST 2
#set output 'cdf-dest2-99.pdf'
#
#plot \
#"CDF_flexcast_99%loc_node2.txt" u 1:(.000036) smooth cumulative w lines t "FlexCast", \
#"CDF_byzcast_99%loc_node2.txt"  u 1:(.000036) smooth cumulative w lines t "ByzCast", \
#"CDF_skeen_99%loc_node2.txt"    u 1:(.000036) smooth cumulative w lines t "Skeen"
#
#### DEST 3
#set output 'cdf-dest3-99.pdf'
#
#plot \
#"CDF_flexcast_99%loc_node3.txt" u 1:(.0036) smooth cumulative w lines t "FlexCast", \
#"CDF_byzcast_99%loc_node3.txt"  u 1:(.0036) smooth cumulative w lines t "ByzCast", \
#"CDF_skeen_99%loc_node3.txt"    u 1:(.0036) smooth cumulative w lines t "Skeen"

