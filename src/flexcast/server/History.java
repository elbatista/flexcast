package flexcast.server;

import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import org.jgrapht.Graph;
import org.jgrapht.alg.shortestpath.BellmanFordShortestPath;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.builder.GraphTypeBuilder;
import org.jgrapht.nio.dot.DOTExporter;
import flexcast.messages.LightMessage;
import flexcast.messages.LightMessagesList;
import flexcast.messages.Message;
import flexcast.messages.LightMessagesList.Item;
import util.BaseObj;

public class History extends BaseObj {
    private short nodeid;
    private ArrayList<Short> ancestors;
    private LightMessagesList history = new LightMessagesList();
    private LightMessagesList fullHistory = new LightMessagesList();
    private HashMap<Integer, PendingMessage> pendingMessages = new HashMap<>();
    private HashMap<Integer, Boolean> deliveredMsgs = new HashMap<>();
    private Graph<LightMessage, DefaultEdge> globalHstGraph;
    private BellmanFordShortestPath<LightMessage, DefaultEdge> bellmanFordShortestPath;
    private HashMap<Short, LightMessagesList> hstAncestors = new HashMap<>();
    private HashMap<Short, LightMessagesList> hstAncestorsMsgsToMe = new HashMap<>();
    private HashMap<Short, HashMap<Integer, Boolean>> hstAncDeliveredMsgs = new HashMap<>();

    public History(ArrayList<Short> ancestors, short nodeid) {
        globalHstGraph = GraphTypeBuilder.<LightMessage, DefaultEdge> directed()
        .allowingMultipleEdges(false)
        .allowingSelfLoops(false)
        .weighted(false)
        .edgeClass(DefaultEdge.class)
        .buildGraph();
        bellmanFordShortestPath = new BellmanFordShortestPath<>(globalHstGraph);
        this.ancestors = ancestors;
        this.nodeid = nodeid;
        for(short a : this.ancestors){
            hstAncestors.put(a, new LightMessagesList());
            hstAncestorsMsgsToMe.put(a, new LightMessagesList());
            hstAncDeliveredMsgs.put(a, new HashMap<>());
        }
    }

    public HashMap<Integer, PendingMessage> getPendMsgs() {
        return pendingMessages;
    }

    public HashMap<Integer, Boolean> getDeliveredMsgs() {
        return deliveredMsgs;
    }

    public LightMessagesList getMyHst() {
        return history;
    }

    public LightMessagesList getMyFullHst() {
        return fullHistory;
    }

    public LightMessagesList getAncHst(short anc) {
        return hstAncestors.get(anc);
    }

    public LightMessagesList getAncHstToMe(short anc) {
        return hstAncestorsMsgsToMe.get(anc);
    }

    public PendingMessage getPendMsg(int id) {
        return pendingMessages.get(id);
    }

    public void addPendMsg(int id, PendingMessage pend) {
        pendingMessages.put(id, pend);
    }

    public int getGraphSize() {
        return globalHstGraph.vertexSet().size();
    }

    public boolean messageM1preceedesM2(LightMessage m1, LightMessage m2){
        return (bellmanFordShortestPath.getPath(m1, m2) != null);
    }

    public void addHst(Message m) {
        for(short anc : m.getHst().keySet()){
            Item i = m.getHst().get(anc).getFirst();
            while(i != null){

                LightMessage lm = i.get();
                boolean isAddressedToMe = lm.isAddressedTo(nodeid);

                if(hstAncDeliveredMsgs.get(anc).get(lm.getId()) == null){
                    hstAncestors.get(anc).add(lm);
                    if(isAddressedToMe) hstAncestorsMsgsToMe.get(anc).add(lm);
                    hstAncDeliveredMsgs.get(anc).put(lm.getId(), true);
                }

                globalHstGraph.addVertex(lm);
                if(i.getPrev() != null){
                    globalHstGraph.addEdge(i.getPrev().get(), lm);
                }
                i = i .getNext();
            }
        }
    }

    public String getGraphString() {
        DOTExporter<LightMessage, DefaultEdge> exporter = new DOTExporter<>(v->String.valueOf(v.getId()));
        Writer writer = new StringWriter();
        exporter.exportGraph(globalHstGraph, writer);
        return writer.toString();
    }

    public void addToMyHst(Message m) {
        LightMessage lm = new LightMessage(m.getId(), m.getDst());
        history.add(lm);
        fullHistory.add(lm);
        Item last = history.getLast();
        globalHstGraph.addVertex(lm);
        if(last.getPrev() != null){
            globalHstGraph.addEdge(last.getPrev().get(), lm);
        }
    }

    public void rebuildGraph() {
        // reconstruo o grafo global com base nos hsts atualizados
        globalHstGraph = GraphTypeBuilder.<LightMessage, DefaultEdge> directed()
        .allowingMultipleEdges(false)
        .allowingSelfLoops(false)
        .weighted(false)
        .edgeClass(DefaultEdge.class)
        .buildGraph();

        for(LightMessagesList list : hstAncestors.values()){
            Item item = list.getFirst();
            while(item != null){
                globalHstGraph.addVertex(item.get());
                if(item.getPrev() != null){
                    globalHstGraph.addEdge(item.getPrev().get(), item.get());
                }
                item = item.getNext();
            }
        }
        Item item = history.getFirst();
        while(item != null){
            globalHstGraph.addVertex(item.get());
            if(item.getPrev() != null){
                globalHstGraph.addEdge(item.getPrev().get(), item.get());
            }
            item = item.getNext();
        }
        bellmanFordShortestPath = new BellmanFordShortestPath<>(globalHstGraph);
    }
}
