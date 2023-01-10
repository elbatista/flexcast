package datastructures;

import java.io.Serializable;
import java.util.ArrayList;
import messages.LightMessage;
import util.BaseObj;

@SuppressWarnings("unchecked")
public class MessageDepGraph extends BaseObj implements Serializable {
    private ArrayList<LightMessage> [] graph;

    public MessageDepGraph(short nodes){
        graph = new ArrayList[nodes];
        for (short i = 0; i < nodes; i++)
            graph[i] = new ArrayList<LightMessage>();
    }

    public void add(LightMessage m, short node){
        graph[node].add(m);
    }

    public ArrayList<LightMessage>[] getGraph() {
        return graph;
    }
}
