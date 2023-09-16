package util;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.StringTokenizer;
import java.util.concurrent.TimeUnit;
import com.google.common.math.Quantiles;
import com.google.common.math.Stats;

@SuppressWarnings("unused")
public class Results {
    static int totalFiles = 0;
    static int lines = 0;
    private static long second = 1000000000;

    static class TPLine{
        int clients, skeen, byz, flex;
        public TPLine(int clients, int skeen, int byz, int flex) {
            this.clients = clients;
            this.skeen = skeen;
            this.byz = byz;
            this.flex = flex;
        }
    }

    static ArrayList<Double> readFiles(String strpath){
        ArrayList<Double> values = new ArrayList<>();
        try {
            Files.list(Paths.get(strpath)) 
            .filter(file -> {try{return !Files.isHidden(file) && !Files.isDirectory(file);}catch (Exception e) {return false;}})
            .forEach(path -> {
                // System.out.println("Reading file: " + path.getFileName());
                if(path.getFileName().toString().contains("per-node")) return;
                totalFiles++;
                ArrayList<Double> auxvalues = new ArrayList<>();
                Scanner scan = null;
                try{scan = new Scanner(path.toFile());}catch (Exception e) {}
                while(scan.hasNext()){
                    String line = scan.nextLine();
                    if(line.startsWith("O") || line.equals("")) continue;
                    if(line.startsWith("-")) break;

                    StringTokenizer str = new StringTokenizer(line, "\t");
                    if(str.countTokens() > 2){
                        str.nextToken(); // skip the first column (ORDER)
                        auxvalues.add(Double.valueOf(TimeUnit.MICROSECONDS.toMillis(Long.valueOf(str.nextToken())))); // add the second column (LATENCY)
                    }

                }
                values.addAll(auxvalues.subList((int)(auxvalues.size() * 0.1), (int)(auxvalues.size() * 0.9)));
                //values.addAll(auxvalues);
                auxvalues.clear();
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
        return values;
    }

    private static double readTPFiles(String strpath) {
        ArrayList<Double> values = new ArrayList<>();
        ArrayList<Double> valuesperclient = new ArrayList<>();
        try {
            Files.list(Paths.get(strpath)) 
            .filter(file -> {try{return !Files.isHidden(file) && !Files.isDirectory(file);}catch (Exception e) {return false;}})
            .forEach(path -> {
                if(!path.getFileName().toString().contains("client")) return;
                totalFiles++;
                Scanner scan = null;
                try{scan = new Scanner(path.toFile());}catch (Exception e) {}
                while(scan.hasNext()){
                    String line = scan.nextLine();
                    if(line.startsWith("Tp at sec") && !line.startsWith("Tp at sec 60")){
                        lines++;
                        StringTokenizer str = new StringTokenizer(line, ":");
                        str.nextToken(); // skip the first column (text)
                        values.add(Double.valueOf(str.nextToken().trim())); // add the second column (tp)
                    }
                }
                valuesperclient.add(Stats.of(values.subList((int)(values.size() * .1), (int)(values.size() * .9))).mean());
                values.clear();
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
        // System.out.println(strpath + " - " + Quantiles.scale(100).indexes(5,25,50,75,80,90,95,99).compute(valuesperclient));
        return Stats.of(valuesperclient).sum();
    }

    static ArrayList<ArrayList<Double>> readFilesPerNode(String strpath, short numNodes){
        ArrayList<ArrayList<Double>> values = new ArrayList<>();
        try {
            Files.list(Paths.get(strpath)) 
            .filter(file -> {try{return !Files.isHidden(file) && !Files.isDirectory(file);}catch (Exception e) {return false;}})
            .forEach(path -> {
                if(!path.getFileName().toString().contains("per-node")) return;

                totalFiles++;
                ArrayList<ArrayList<Double>> auxvalues = new ArrayList<>();
                Scanner scan = null;
                try{scan = new Scanner(path.toFile());}catch (Exception e) {}
                while(scan.hasNext()){
                    String line = scan.nextLine();
                    if(line.startsWith("O") || line.equals("")) continue; // pula linha de cabecalho
                    if(line.startsWith("-")) break; // para quando comeca o resumo

                    StringTokenizer str = new StringTokenizer(line, "\t");
                    // EXEMPLOS de Linhas
                    // ORDER	LAT_0	LAT_1	LAT_2	LAT_3	LAT_4	LAT_5	LAT_6	LAT_7	LAT_8	LAT_9	LAT_10	LAT_11	DSTS	    TYPE
                    // 58	    2102	2320	12634	0	    0	    0	    0	    0	    0	    0	    0	    0	    [0,1,2]	    global
                    // 41	    0	    0	    0	    0	    19251	0	    23630	35903	0	    0	    0	    0	    [4,6,7]	    global
                    // 14	    0	    0	    0	    0	    0	    5072	0	    0	    0	    1667	0	    0	    [5,9]	    global

                    if(str.countTokens() > 2){
                        str.nextToken(); // skip the first column (ORDER)

                        ArrayList<Double> nodeValues = new ArrayList<>();
                        for(short n = 0; n < numNodes; n++){
                            String val = str.nextToken();
                            if(Double.valueOf(val) > 0){
                                nodeValues.add(Double.valueOf(TimeUnit.MICROSECONDS.toMillis(Long.valueOf(val))));
                            }
                            ;
                        }

                        auxvalues.add(nodeValues); // add the second column (LATENCY)
                    }

                }
                values.addAll(auxvalues.subList((int)(auxvalues.size() * 0.1), (int)(auxvalues.size() * 0.9)));
                // values.addAll(auxvalues);
                auxvalues.clear();
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
        return values;
    }

    private static void writeTPFile(HashMap<Integer, HashMap<String, Double>> tpValues, short nodes, String locality, int gc) {
        try {
            PrintWriter printerOut = new PrintWriter("plots/tp/TP_"+nodes+"nodes_"+locality+"%_gc"+gc+"-aws-loc-file-90%.txt");
            ArrayList<Integer> sortedKeys = new ArrayList<Integer>(tpValues.keySet());
            Collections.sort(sortedKeys);
            for(int cli : sortedKeys){
                printerOut.println(
                    cli + 
                    "\t" + tpValues.get(cli).get("skeen_gc"+gc)+ 
                    "\t" + tpValues.get(cli).get("byzcast_gc"+gc)+ 
                    "\t" + tpValues.get(cli).get("flexcast_gc"+gc)
                );
            }
            printerOut.flush();
            printerOut.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private static void writeCDFFiles(HashMap<Short, ArrayList<Double>> values, String algo, String locality) {
        try {
            for(ArrayList<Double> a : values.values()) a.sort(Double::compare);
            ArrayList<Double> [] array = new ArrayList[values.size()];
            int i = 0;
            for(ArrayList<Double> a : values.values()){
                array[i] = a;
                i++;
            }
         
            PrintWriter printerOut = new PrintWriter("CDF_"+algo+"_"+locality+"%loc_node1.txt");
            for(double v: array[0]) printerOut.println(v);
            printerOut.flush();
            printerOut.close();

            printerOut = new PrintWriter("CDF_"+algo+"_"+locality+"%loc_node2.txt");
            for(double v: array[1]) printerOut.println(v);
            printerOut.flush();
            printerOut.close();

            printerOut = new PrintWriter("CDF_"+algo+"_"+locality+"%loc_node3.txt");
            for(double v: array[2]) printerOut.println(v);
            printerOut.flush();
            printerOut.close();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private static void processMsgSizeFiles(String strpath, short nodes, String locality, int gc, int cli, String algo) {
        int[] qty = new int[nodes];
        int[] avgsize = new int[nodes];
        try {
            Files.list(Paths.get(strpath)) 
            .filter(file -> {try{return !Files.isHidden(file) && !Files.isDirectory(file);}catch (Exception e) {return false;}})
            .forEach(path -> {
                if(!path.getFileName().toString().contains("MsgSizes")) return;
                short node = Short.valueOf(path.getFileName().toString().replaceAll("[^0-9]", ""));
                Scanner scan = null;
                int qtyMsgs=0;
                double size=0;
                try{scan = new Scanner(path.toFile());}catch (Exception e) {}
                while(scan.hasNext()){
                    String line = scan.nextLine();
                    StringTokenizer str = new StringTokenizer(line, ";");
                    size += Double.valueOf(str.nextToken()); // skip the first column (text)
                    qtyMsgs++;
                }
                qty[node] = qtyMsgs;
                avgsize[node] = (int)(size/qtyMsgs);
            });
            
            PrintWriter printerOut = new PrintWriter("plots/msgsizes/"+algo+"_"+nodes+"nodes_"+cli +"cli_"+locality+"%_gc"+gc+"-aws-loc-file-90%.txt");
            
            for(int i=0; i < nodes; i++){
                printerOut.println(i + "\t" + qty[i] + "\t" + avgsize[i]);
                // System.out.println("Node"+node+": "+qtyMsgs+" msgs (avg "+(int)(size/qtyMsgs)+" bytes each)");
            }
            printerOut.flush();
            printerOut.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void processMsgSizeFilesDiscrete(String strpath, short nodes, String locality, int gc, int cli, String algo, short [] nodeMap) {
        
        HashMap<Integer, ArrayList<MsgSize>> values = new HashMap<>();

        try {
            Files.list(Paths.get(strpath)) 
            .filter(file -> {try{return !Files.isHidden(file) && !Files.isDirectory(file);}catch (Exception e) {return false;}})
            .forEach(path -> {
                if(!path.getFileName().toString().contains("MsgSizes")) return;
                short node = Short.valueOf(path.getFileName().toString().replaceAll("[^0-9]", ""));
                values.put((int)node, new ArrayList<>());
                Scanner scan = null;
                long time;
                int id;
                double size=0;
                try{scan = new Scanner(path.toFile());}catch (Exception e) {}
                while(scan.hasNext()){
                    String line = scan.nextLine();
                    StringTokenizer str = new StringTokenizer(line, ";");
                    time = Long.valueOf(str.nextToken()); 
                    id = Integer.valueOf(str.nextToken()); 
                    size = Double.valueOf(str.nextToken()); 
                    values.get((int)node).add(new MsgSize(time, id, size, null));
                }
            });

            PrintWriter printerOut = new PrintWriter("plots/msgsizes/"+algo+"_"+nodes+"nodes_"+cli +"cli_"+locality+"%_gc"+gc+"-aws-loc-file-90%___v2.txt");

            for(int node : values.keySet()){
                List<MsgSize> l = values.get(node);
                List<MsgSize> sub = l.subList((int)(l.size()*.1), (int)(l.size()-(l.size()*.1)));
                long iniTime = sub.get(0).getTime();
                int qty = 0;
                double size = 0;
                System.out.println("Node" + node + " ---------");
                ArrayList<Integer> qtyPerSec = new ArrayList<>();
                ArrayList<Double> sizePerSec = new ArrayList<>();
                for(MsgSize m : sub){
                    if((m.getTime()-iniTime) >= second){
                        System.out.println(qty + " msgs/sec; " + ((size > 0 && qty > 0) ? size/qty : 0) + " bytes each (avg)");
                        qtyPerSec.add(qty);
                        sizePerSec.add((size > 0 && qty > 0) ? size/qty : 0);
                        iniTime = m.getTime();
                        qty = 0;
                        size = 0;
                    }
                    else {
                        qty++;
                        size += m.getSize();
                    }
                }
                if(qtyPerSec.size() > 0 && sizePerSec.size() > 0){
                    // System.out.println(Stats.of(qtyPerSec).mean() + " msg/sec; " + Stats.of(sizePerSec).mean() +" bytes each (avg)");
                    printerOut.println(nodeMap[node] + "\t" + Stats.of(qtyPerSec).mean() + "\t" + Stats.of(sizePerSec).mean());
                }
            }
            
            // for(int i=0; i < nodes; i++){
            //     printerOut.println(i + "\t" + qty[i] + "\t" + avgsize[i]);
            //     // System.out.println("Node"+node+": "+qtyMsgs+" msgs (avg "+(int)(size/qtyMsgs)+" bytes each)");
            // }
            printerOut.flush();
            printerOut.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String ... args){
        // ArrayList<Double> latencies = new ArrayList<>();
        
        String localities [] = {"99"};
        short numnodes []    = {12};
        String algos []      = {"flexcast", "skeen", "byzcast"};
        int clients []       = {192};//{12, 24, 48, 96, 192, 384, 768};
        int gc               = 0;

        ArrayList<TPLine> tp = new ArrayList<>();
        HashMap<Integer, HashMap<String, Double>> tpValues = new HashMap<>();
        short [] nodeMap;
        for(String locality : localities){
            for(short nodes : numnodes){
                tpValues = new HashMap<>();
                nodeMap = new short[nodes];
                for(String algo : algos){
                    for(int cli : clients){
                        
                        String basedir ="experiments/"+algo+"-aws-loc-file-90%/"+nodes+"nodes/"+cli+"cli/"+locality+"%/gc"+gc;
                        loadNodesMap(nodeMap, basedir);
                        
                        // ###################### Throughput ######################
                        // double avgtp = 0;
                        // totalFiles = 0;
                        // avgtp += readTPFiles(basedir+"/logs");
                        // System.out.println("TP - Read "+totalFiles+" tp files. Avg "+lines/totalFiles+" lines per file");
                        // System.out.println(basedir);
                        // System.out.println("AVG Throughput: "+avgtp+" ops/sec");
                        // if(tpValues.get(cli) == null) tpValues.put(cli, new HashMap<>());
                        // tpValues.get(cli).put(algo+"_gc"+gc, avgtp);

                        // ###################### Msg Sizes ######################
                        processMsgSizeFilesDiscrete(basedir+"/files", nodes, locality, gc, cli, algo, nodeMap);
                    }
                    
                }
                // writeTPFile(tpValues, nodes, locality, gc);
            }
        }

        
        // latencies.addAll(readFiles("consolid/"+algo+"/"+nodes+"nodes/"+dur+"s/"+cli+"cli/"+locality+"%/results/america"));
        // latencies.addAll(readFiles("consolid/"+algo+"/"+nodes+"nodes/"+dur+"s/"+cli+"cli/"+locality+"%/results/europe"));
        // latencies.addAll(readFiles("consolid/"+algo+"/"+nodes+"nodes/"+dur+"s/"+cli+"cli/"+locality+"%/results/asia"));

        // System.out.println(algo+"/"+nodes+"nodes/"+dur+"s/"+cli+"cli/"+locality+"%" + "\nRead "+totalFiles+" latency files...");
        // if(latencies.size()>0) 
        //     System.out.println("AVG Lat: " + Stats.of(latencies).mean() + "\t" + Quantiles.scale(100).indexes(5,25,50,75,80,90,95,99).compute(latencies));
        
        //////////// LAT PER NODE
        // totalFiles = 0;
        // HashMap<Short, ArrayList<Double>> values = new HashMap<>();
        // for(short n = 0; n < nodes; n++) values.put(n, new ArrayList<>());

        // for(ArrayList<Double> nodesLat : readFilesPerNode(basedir+"/results", nodes)){
        //     for(short i = 0; i < nodesLat.size(); i++){
        //         values.get(i).add(nodesLat.get(i));
        //     }
        // }

        // System.out.println(algo+"/"+nodes+"nodes/gc"+gc+"/"+cli+"cli/"+locality+"%" + "\nRead "+totalFiles+" latency files...");
        // for(short n = 0; n < nodes; n++) 
        //     if(values.get(n).size()>0) 
        //         System.out.println("Node " + n + ": " + Stats.of(values.get(n)).mean()  + "(" + Stats.of(values.get(n)).sampleStandardDeviation() + ")" + "\t" + Quantiles.scale(100).indexes(5,25,50,75,80,90,95,99).compute(values.get(n)));
        
        // // CFDs
        // writeCDFFiles(values, algo, locality);

        
    }

    private static void loadNodesMap(short[] nodeMap, String basedir) {
        Scanner scan = null;
        int index = 0;
        try{scan = new Scanner(Paths.get(basedir+"/config/servers.conf"));}catch (Exception e) {}
        while(scan.hasNext()){
            String line = scan.nextLine();
            if(line.startsWith("#")) continue;
            StringTokenizer str = new StringTokenizer(line, ",");
            short node = Short.valueOf(str.nextToken().replaceAll("[^0-9]", ""));
            nodeMap[index] = node;
            index++;
        }
    }

}
