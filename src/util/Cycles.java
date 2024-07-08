package util;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;
import org.javatuples.Pair;
import org.jgrapht.Graph;
import org.jgrapht.alg.cycle.CycleDetector;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.builder.GraphBuilder;
import org.jgrapht.graph.builder.GraphTypeBuilder;

public class Cycles {

    private class LightMessage  {
        private String id = "";
        public LightMessage(String id){
            this.id = id;
        }
        public String getId() { return id; }
    }    

    private HashMap<Short, ArrayList<LightMessage>> allMessages = new HashMap<>();
    private short[] nodes = new short[]{0};
    protected String toString(Object... args){
        String s = "";
        for(Object obj : args)s+=String.valueOf((obj==null?"":obj))+" ";
        return s;
    }
    protected void print(Object... args){System.out.println(toString(args));}

    public ArrayList<LightMessage> loadMessages(short id, boolean cli) {
        try {
            Scanner scan=null;
            ArrayList<LightMessage> list = new ArrayList<>();
            try {
                scan = new Scanner(new File("TO_vio_consumer.txt"));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            // skip header
            String line = scan.nextLine();
            while(scan.hasNextLine()){
                line = scan.nextLine();

                String [] aux = line.split("\t");
                
                list.add(new LightMessage(aux[0].trim()));
            }

            return list;
        } catch (Exception ex) {
            ex.printStackTrace();
           return null;
        }
    }

    public void verifyCycles(){

        int totalMsgs = 0;
        // load messages from files
        for(short n : nodes){
            ArrayList<LightMessage> msgs = loadMessages(n, false);
            if(msgs != null){
                totalMsgs += msgs.size();
                allMessages.put(n, msgs);
            }
            else {
                print("Error !!! Could not find messages for node", n, "!!!");
                System.exit(0);
            }
        }
        
        Graph<String,DefaultEdge> globalGraph = 
        GraphTypeBuilder.<String, DefaultEdge> directed()
        .allowingMultipleEdges(false)
        .allowingSelfLoops(false)
        .weighted(false)
        .edgeClass(DefaultEdge.class)
        .buildGraph();

        GraphBuilder<String,DefaultEdge,Graph<String,DefaultEdge>> builder = 
        new GraphBuilder<String,DefaultEdge,Graph<String,DefaultEdge>>(globalGraph);

        Set<Pair<String, String>> graphViz = new HashSet<>();
        
        // load the graph with messages delivered by each node
        for(short n : nodes){
            ArrayList<LightMessage> delivered = allMessages.get(n);
            boolean first = true;
            for(int idx = 0; idx < delivered.size(); idx++){
                builder.addVertex(delivered.get(idx).getId());
                if(!first) {
                    builder.addEdge(delivered.get(idx-1).getId(), delivered.get(idx).getId());
                    graphViz.add(new Pair<String,String>(delivered.get(idx-1).getId(), delivered.get(idx).getId()));
                }
                first = false;
            }
        }

        Graph<String, DefaultEdge> graph = builder.build();
        
        print("Cycle Validation Result:");
        print("-------------------------");
        print();

        print("Num Nodes:", nodes.length);
        print("Total msgs:", totalMsgs);
        print("Graph size:", graph.vertexSet().size());
        print();

        boolean cycle = new CycleDetector<>(graph).detectCycles();
        print("-------------------------");
        print("Contains cycle:", cycle);
        print("-------------------------");

        if(cycle){
            print("digraph G {");
            for(Pair<String,String> pair : graphViz){
                print("   ",pair.getValue0(),"->",pair.getValue1(), ";");
            }
            print("}");
        }

    }

    public static void main(String args []){
        new Cycles().verifyCycles();
    }
}
