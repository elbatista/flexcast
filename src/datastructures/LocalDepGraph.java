package datastructures;

import java.util.ArrayList;
import java.util.HashMap;

// import org.jgrapht.Graph;
// import org.jgrapht.alg.cycle.CycleDetector;
// import org.jgrapht.graph.DefaultEdge;
// import org.jgrapht.graph.builder.GraphBuilder;
// import org.jgrapht.graph.builder.GraphTypeBuilder;
import messages.LightMessage;
import messages.LightMessagesList;
import messages.Message;
import messages.LightMessagesList.Item;
import messages.LightMessagesList.ItemHst;
import util.BaseObj;

public class LocalDepGraph extends BaseObj {
    private LightMessagesList [] graph;
    private short node;
    private Item updateDGPointers[][];
    private HashMap<Integer, Boolean> [] inserted;

    public LocalDepGraph(short node){
        this.node = node;
        graph = new LightMessagesList[node+1];
        inserted = new HashMap[node+1];
        for (short i = 0; i <= node; i++){
            graph[i] = new LightMessagesList();
            inserted[i] = new HashMap<>();
        }
        if(node > 0) updateDGPointers = new Item[node][node];
    }

    public LightMessagesList getMyHst(){
        return graph[this.node];
    }

    public LightMessagesList [] getGraph() {
        return graph;
    }
 
    public ItemHst addToHst(LightMessage m){
        return graph[this.node].addHst(m);
    }

    public void update(Message m) {
        ArrayList<LightMessage> [] mgraph = m.getDepGraph().getGraph();
        for(short i = 0; i < mgraph.length; i++){
            if(mgraph[i].size() > 0){
                addNewMessagesToLocalGraph(i, mgraph[i], m.getSender());
            }
        }
	}

    private void addNewMessagesToLocalGraph(short n, ArrayList<LightMessage> mgraph, short sender) {
        Item item = updateDGPointers[sender][n] == null ? graph[n].getFirst() : updateDGPointers[sender][n];
        int mgraphidx = 0;
        while(item != null){
            if(item.get().getId() == mgraph.get(mgraphidx).getId()){
                mgraphidx++;
                if(mgraphidx == mgraph.size()) break;
            }
            item = item.getNext();
        }
        for(int i = mgraphidx; i < mgraph.size(); i++){
            if(inserted[n].get(mgraph.get(i).getId())==null) {
                graph[n].add(mgraph.get(i));
                inserted[n].put(mgraph.get(i).getId(), true);
            }
        }

        updateDGPointers[sender][n] = item;
    }

    public String getDepGraphAsString() {
        String s = "[";
        if(graph != null){
            for(int i = 0; i < graph.length; i++){
                s += i + "-{";
                LightMessagesList.Item item = graph[i].getFirst();
                while(item != null){
                    s += item.get().getId();
                    item = item.getNext();
                    if(item != null) s += ", ";
                }
                s += "}  ";
            }
        }
        return s + "]";
    }

}
