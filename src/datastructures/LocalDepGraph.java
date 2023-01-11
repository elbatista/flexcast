package datastructures;

import java.util.ArrayList;
import java.util.HashMap;
import org.jgrapht.Graph;
import org.jgrapht.alg.cycle.CycleDetector;
import org.jgrapht.graph.DefaultEdge;
// import org.jgrapht.graph.builder.GraphBuilder;
import org.jgrapht.graph.builder.GraphTypeBuilder;
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
    Graph<Integer,DefaultEdge> globalGraph2;
    HashMap<Integer, HashMap<Integer, Boolean>> cyclesDetected;

    public LocalDepGraph(short node){//, Item[] dgPointers){
        this.node = node;
        graph = new LightMessagesList[node+1];
        for (short i = 0; i <= node; i++)  graph[i] = new LightMessagesList();
        if(node > 0) updateDGPointers = new Item[node][node];
        cyclesDetected = new HashMap<>();
        globalGraph2 = 
        GraphTypeBuilder.<Integer, DefaultEdge> directed()
        .allowingMultipleEdges(false)
        .allowingSelfLoops(false)
        .weighted(false)
        .edgeClass(DefaultEdge.class)
        .buildGraph();
    }

    public LightMessagesList getMyHst(){
        return graph[this.node];
    }

    public LightMessagesList [] getGraph() {
        return graph;
    }
 
    public ItemHst addToHst(LightMessage m){
        ItemHst item = graph[this.node].addHst(m);
        globalGraph2.addVertex(m.getId());
        if(getMyHst().getLast().getPrev() != null){
            globalGraph2.addEdge(getMyHst().getLast().getPrev().get().getId(), m.getId());
        }
        return item;
    }

    public boolean doesMessageComesFirstThan(short lcd, LightMessage m, LightMessage m2){
        // LightMessagesList.Item item = dgPointers[lcd] == null ? graph[lcd].getFirst() : dgPointers[lcd];
        LightMessagesList.Item item = graph[lcd].getFirst();
        while(item != null){
            // if m comes first ... returns true
            if(item.get().getId() == m.getId()) return true;
            // if didnt found m yet, and finds m2 ... returns false
            if(item.get().getId() == m2.getId()) return false;
            item = item.getNext();
        }
        // if neither m and m2 are present, returns false ...
        return false;
    }

    public void update(Message m) {
        ArrayList<LightMessage> [] mgraph = m.getDepGraph().getGraph();
        for(short i = 0; i < mgraph.length; i++)
            if(mgraph[i].size() > 0)
                addNewMessagesToLocalGraph(i, mgraph[i], m.getSender());
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
            graph[n].add(mgraph.get(i));
            globalGraph2.addVertex(mgraph.get(i).getId());
            if(graph[n].getLast().getPrev() != null){
                globalGraph2.addEdge(graph[n].getLast().getPrev().get().getId(), mgraph.get(i).getId());
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

    public boolean generatesCycleOnDelivering(LightMessage m1, LightMessage m2, Item[] dgPointers, ItemHst[] dgPointersHst) {
        // Graph<Integer,DefaultEdge> globalGraph = 
        // GraphTypeBuilder.<Integer, DefaultEdge> directed()
        // .allowingMultipleEdges(false)
        // .allowingSelfLoops(false)
        // .weighted(false)
        // .edgeClass(DefaultEdge.class)
        // .buildGraph();

        // GraphBuilder<Integer,DefaultEdge,Graph<Integer,DefaultEdge>> builder = 
        // new GraphBuilder<Integer,DefaultEdge,Graph<Integer,DefaultEdge>>(globalGraph);
        // boolean first;
        // for(short i = 0; i < node; i++){
        //     // Item item = dgPointers[i] == null ? graph[i].getFirst() : dgPointers[i];
        //     Item item = graph[i].getFirst();
        //     first = true;
        //     while(item != null){
        //         builder.addVertex(item.get().getId());
        //         if(!first) builder.addEdge(item.getPrev().get().getId(), item.get().getId());
        //         item = item.getNext();
        //         first = false;
        //     }
        // }

        // // ItemHst itemHst = dgPointersHst[0];
        // // for(short i = 1; i < dgPointersHst.length; i++){
        // //     if(itemHst == null){
        // //         itemHst = dgPointersHst[i];
        // //         continue;
        // //     }
        // //     if(dgPointersHst[i] != null && dgPointersHst[i].getIdx() < itemHst.getIdx())
        // //         itemHst = dgPointersHst[i];
        // // }
        // // if(itemHst == null) itemHst = (ItemHst) getMyHst().getFirst();
        // ItemHst itemHst = (ItemHst) getMyHst().getFirst();
        // first = true;
        // while(itemHst != null){
        //     builder.addVertex(itemHst.get().getId());
        //     if(!first) builder.addEdge(itemHst.getPrev().get().getId(), itemHst.get().getId());
        //     itemHst = (ItemHst)itemHst.getNext();
        //     first = false;
        // }

        // if(graph[node].getLast() != null)
        //     builder.addEdgeChain(graph[node].getLast().get().getId(), m1.getId(), m2.getId());
        // else
        //     builder.addEdgeChain(m1.getId(), m2.getId());

        // return new CycleDetector<>(builder.build()).detectCycles();




        ////////
        if(cyclesDetected.get(m1.getId()) != null){
            if(cyclesDetected.get(m1.getId()).get(m2.getId()) != null) return true;
        }

        boolean m1exists = !globalGraph2.addVertex(m1.getId());
        boolean m2exists = !globalGraph2.addVertex(m2.getId());
        boolean addedEdgeHst = false;

        globalGraph2.addEdge(m1.getId(), m2.getId());

        if(getMyHst().getLast() != null){
            addedEdgeHst = true;
            globalGraph2.addEdge(getMyHst().getLast().get().getId(), m1.getId());
        }

        boolean cycle = new CycleDetector<>(globalGraph2).detectCycles();

        if(cycle){
            if(cyclesDetected.get(m1.getId()) == null) cyclesDetected.put(m1.getId(), new HashMap<>());
            cyclesDetected.get(m1.getId()).put(m2.getId(), cycle);
        }

        if(addedEdgeHst) globalGraph2.removeEdge(getMyHst().getLast().get().getId(), m1.getId());
        globalGraph2.removeEdge(m1.getId(), m2.getId());
        if(!m2exists) globalGraph2.removeVertex(m2.getId());
        if(!m1exists) globalGraph2.removeVertex(m1.getId());

        return cycle;
    }

}
