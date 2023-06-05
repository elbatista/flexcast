package util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import java.util.StringTokenizer;
import java.util.concurrent.TimeUnit;

import com.google.common.math.Quantiles;
import com.google.common.math.Stats;

public class Results {
    static int totalFiles = 0;
    static int lines = 0;
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
                        auxvalues.add(Double.valueOf(str.nextToken())); // add the second column (LATENCY)
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


    public static void main(String ... args){
        ArrayList<Double> latencies = new ArrayList<>();
        
        String algo     = "byz/tree3";
        String locality = "100";
        short nodes     = 12;
        int dur         = 60;
        int cli         = 192;

        // latencies.addAll(readFiles("consolid/"+algo+"/"+nodes+"nodes/"+dur+"s/"+cli+"cli/"+locality+"%/results/america"));
        // latencies.addAll(readFiles("consolid/"+algo+"/"+nodes+"nodes/"+dur+"s/"+cli+"cli/"+locality+"%/results/europe"));
        // latencies.addAll(readFiles("consolid/"+algo+"/"+nodes+"nodes/"+dur+"s/"+cli+"cli/"+locality+"%/results/asia"));

        HashMap<Short, ArrayList<Double>> values = new HashMap<>();
        for(short n = 0; n < nodes; n++) values.put(n, new ArrayList<>());

        for(ArrayList<Double> nodesLat : readFilesPerNode("consolid/"+algo+"/"+nodes+"nodes/"+dur+"s/"+cli+"cli/"+locality+"%/results/america", nodes)){
            for(short i = 0; i < nodesLat.size(); i++){
                values.get(i).add(nodesLat.get(i));
            }
        }
        
        for(ArrayList<Double> nodesLat : readFilesPerNode("consolid/"+algo+"/"+nodes+"nodes/"+dur+"s/"+cli+"cli/"+locality+"%/results/europe", nodes)){
            for(short i = 0; i < nodesLat.size(); i++){
                values.get(i).add(nodesLat.get(i));
            }
        }
        
        for(ArrayList<Double> nodesLat : readFilesPerNode("consolid/"+algo+"/"+nodes+"nodes/"+dur+"s/"+cli+"cli/"+locality+"%/results/asia", nodes)){
            for(short i = 0; i < nodesLat.size(); i++){
                values.get(i).add(nodesLat.get(i));
            }
        }
        
        System.out.println(algo + " - Read "+totalFiles+" latency files...");
        for(short n = 0; n < nodes; n++) 
            if(values.get(n).size()>0) 
                System.out.println("Node " + n + ": " + Stats.of(values.get(n)).mean() + "\t" + Quantiles.scale(100).indexes(5,25,50,75,80,90,95,99).compute(values.get(n)));
        
        //////////// TP
        // double avgtp = 0;
        // totalFiles = 0;

        // avgtp += readTPFiles("consolid/"+algo+"/"+nodes+"nodes/"+dur+"s/"+cli+"cli/"+locality+"%/logs/clients/america");
        // avgtp += readTPFiles("consolid/"+algo+"/"+nodes+"nodes/"+dur+"s/"+cli+"cli/"+locality+"%/logs/clients/europe");
        // avgtp += readTPFiles("consolid/"+algo+"/"+nodes+"nodes/"+dur+"s/"+cli+"cli/"+locality+"%/logs/clients/asia");

        // System.out.println("Read "+totalFiles+" tp files. Avg "+lines/totalFiles+" lines per file");
        // System.out.println("AVG Throughput: "+avgtp+" ops/sec");
    }
}
