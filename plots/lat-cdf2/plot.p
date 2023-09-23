set terminal pdf dashed size 5, 3 font ',20'
set key right bottom
set grid ytics lt 0 lw 1
set xtics out nomirror rotate by 60 right
set xlabel "Latency (ms)"
set ylabel "CDF"
set xrange[0:500]

set ytics(0, 0.2, 0.4, 0.6, 0.8, 1)

##########  90% #########
set output 'lat-cdf2-90-node1.pdf'
plot \
"CDF_flexcast_90%loc_node1.txt" using 2:($1/1000) title "FlexCast" with  linespoint pointinterval  50,  \
"CDF_byzcast_90%loc_node1.txt"  using 2:($1/1000) title "ByzCast" with  linespoint pointinterval 100,  \
"CDF_skeen_90%loc_node1.txt"    using 2:($1/1000) title "Skeen" with  linespoint pointinterval 100

set output 'lat-cdf2-90-node2.pdf'
plot \
"CDF_flexcast_90%loc_node2.txt" using 2:($1/1000) title "FlexCast" with  linespoint pointinterval  50,  \
"CDF_byzcast_90%loc_node2.txt"  using 2:($1/1000) title "ByzCast" with  linespoint pointinterval 100,  \
"CDF_skeen_90%loc_node2.txt"    using 2:($1/1000) title "Skeen" with  linespoint pointinterval 100

set output 'lat-cdf2-90-node3.pdf'
plot \
"CDF_flexcast_90%loc_node3.txt" using 2:($1/100) title "FlexCast" with  linespoint pointinterval  50,  \
"CDF_byzcast_90%loc_node3.txt"  using 2:($1/100) title "ByzCast" with  linespoint pointinterval 100,  \
"CDF_skeen_90%loc_node3.txt"    using 2:($1/100) title "Skeen" with  linespoint pointinterval 100


##########  95% #########
set output 'lat-cdf2-95-node1.pdf'
plot \
"CDF_flexcast_95%loc_node1.txt" using 2:($1/1000) title "FlexCast" with  linespoint pointinterval  50,  \
"CDF_byzcast_95%loc_node1.txt"  using 2:($1/1000) title "ByzCast" with  linespoint pointinterval 100,  \
"CDF_skeen_95%loc_node1.txt"    using 2:($1/1000) title "Skeen" with  linespoint pointinterval 100

set output 'lat-cdf2-95-node2.pdf'
plot \
"CDF_flexcast_95%loc_node2.txt" using 2:($1/1000) title "FlexCast" with  linespoint pointinterval  50,  \
"CDF_byzcast_95%loc_node2.txt"  using 2:($1/1000) title "ByzCast" with  linespoint pointinterval 100,  \
"CDF_skeen_95%loc_node2.txt"    using 2:($1/1000) title "Skeen" with  linespoint pointinterval 100

set output 'lat-cdf2-95-node3.pdf'
plot \
"CDF_flexcast_95%loc_node3.txt" using 2:($1/100) title "FlexCast" with  linespoint pointinterval  50,  \
"CDF_byzcast_95%loc_node3.txt"  using 2:($1/100) title "ByzCast" with  linespoint pointinterval 100,  \
"CDF_skeen_95%loc_node3.txt"    using 2:($1/100) title "Skeen" with  linespoint pointinterval 100




##########  99% #########
set output 'lat-cdf2-99-node1.pdf'
plot \
"CDF_flexcast_99%loc_node1.txt" using 2:($1/1000) title "FlexCast" with linespoint pointinterval  50, \
"CDF_byzcast_99%loc_node1.txt"  using 2:($1/1000) title "ByzCast" with  linespoint pointinterval 100, \
"CDF_skeen_99%loc_node1.txt"    using 2:($1/1000) title "Skeen" with    linespoint pointinterval 100

set output 'lat-cdf2-99-node2.pdf'
plot \
"CDF_flexcast_99%loc_node2.txt" using 2:($1/1000) title "FlexCast" with linespoint pointinterval  50, \
"CDF_byzcast_99%loc_node2.txt"  using 2:($1/1000) title "ByzCast" with linespoint pointinterval 100, \
"CDF_skeen_99%loc_node2.txt"    using 2:($1/1000) title "Skeen" with linespoint pointinterval 100

set output 'lat-cdf2-99-node3.pdf'
plot \
"CDF_flexcast_99%loc_node3.txt" using 2:($1/100) title "FlexCast" with linespoint pointinterval  50, \
"CDF_byzcast_99%loc_node3.txt"  using 2:($1/100) title "ByzCast" with linespoint pointinterval 100, \
"CDF_skeen_99%loc_node3.txt"    using 2:($1/100) title "Skeen" with linespoint pointinterval 100







#######################
#   720 CLIENTS
#######################


##########  99% #########
set output 'lat-cdf2-99-720cli-node1.pdf'
plot \
"CDF_flexcast_720cli_99%loc_node1.txt" using 2:($1/1000) title "FlexCast" with linespoint pointinterval  50, \
"CDF_byzcast_720cli_99%loc_node1.txt"  using 2:($1/1000) title "ByzCast" with  linespoint pointinterval 100, \
"CDF_skeen_720cli_99%loc_node1.txt"    using 2:($1/1000) title "Skeen" with    linespoint pointinterval 100






#######################
#   OVERLAY COMPARISON CDFs
#######################


##########  90% #########
set output 'lat-cdf2-90-flex-overlays-node1.pdf'
plot \
"CDF_flexcast_90%loc_node1.txt"          using 2:($1/1000) title "FlexCast O1" with  linespoint pointinterval  50,  \
"CDF_flexcast_192cli_90%loc_dagtree2_node1.txt" using 2:($1/1000) title "FlexCast O2" with  linespoint pointinterval 100

set output 'lat-cdf2-90-byzcast-overlays-node1.pdf'
plot \
"CDF_byzcast_90%loc_node1.txt"          using 2:($1/1000) title "ByzCast T1" with  linespoint pointinterval  50,  \
"CDF_byzcast_192cli_90%loc_dagtree2_node1.txt" using 2:($1/1000) title "ByzCast T2" with  linespoint pointinterval 100,  \
"CDF_byzcast_192cli_90%loc_dagtree3_node1.txt" using 2:($1/1000) title "ByzCast T3" with  linespoint pointinterval 100
