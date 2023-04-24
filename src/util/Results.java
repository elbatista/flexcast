package util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.StringTokenizer;
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

    public static void main(String ... args){
        ArrayList<Double> latencies = new ArrayList<>();
        
        String algo     = "byzcast/tree4/global";
        String locality = "99";
        int nodes       = 6;
        int dur         = 60;
        int cli         = 360;

        latencies.addAll(readFiles("results/"+algo+"/"+nodes+"nodes/"+dur+"s/"+cli+"cli/"+locality+"%/america"));
        latencies.addAll(readFiles("results/"+algo+"/"+nodes+"nodes/"+dur+"s/"+cli+"cli/"+locality+"%/europe"));
        latencies.addAll(readFiles("results/"+algo+"/"+nodes+"nodes/"+dur+"s/"+cli+"cli/"+locality+"%/asia"));
        
        System.out.println("Read "+totalFiles+" latency files...");
        System.out.println(Stats.of(latencies).mean() + "\t" + Quantiles.scale(100).indexes(5,25,50,75,80,90,95,99).compute(latencies));
        
        //////////// TP
        double avgtp = 0;
        totalFiles = 0;

        avgtp += readTPFiles("results/"+algo+"/"+nodes+"nodes/"+dur+"s/"+cli+"cli/"+locality+"%/logs/clients/america");
        avgtp += readTPFiles("results/"+algo+"/"+nodes+"nodes/"+dur+"s/"+cli+"cli/"+locality+"%/logs/clients/europe");
        avgtp += readTPFiles("results/"+algo+"/"+nodes+"nodes/"+dur+"s/"+cli+"cli/"+locality+"%/logs/clients/asia");

        System.out.println("Read "+totalFiles+" tp files. Avg "+lines/totalFiles+" lines per file");
        System.out.println("AVG Throughput: "+avgtp+" ops/sec");
    }
}
