package flexcast.reconfig.wlot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

public class DAG {

    private int V; // No. of vertices'
    private Map<Integer, List<Edge>> adjs; // array of adjacency lists
    private int dist[][]; // calculated longest distances

    public DAG(List<Integer> dag, List<List<Integer>> weights) // Constructor
    {
        V = dag.size();
        adjs = new HashMap<>();
        dist = new int[V][V];

        for (int i = 0; i < V; ++i) {
            int from = dag.get(i);
            adjs.put(from, new ArrayList<>());
            for (int j = 0; j < V; ++j) {
                dist[i][j] = Integer.MIN_VALUE;
                if (j > i) {
                    int to = dag.get(j);
                    int w = weights.get(from).get(to);
                    adjs.get(from).add(new Edge(to, w));
                }
            }
        }
        computeLongestPaths();
    }

    // A recursive function used by longestPath. See below link for details
    private void topologicalSortUtil(int v, boolean visited[], Stack<Integer> stack) {
        // Mark the current node as visited
        visited[v] = true;

        // Recur for all the vertices adjacent to this vertex
        for (int i = 0; i < adjs.get(v).size(); i++) {
            Edge node = adjs.get(v).get(i);
            if (!visited[node.getTo()])
                topologicalSortUtil(node.getTo(), visited, stack);
        }

        // Push current vertex to stack which stores topological
        // sort
        stack.push(v);
    }

    private void computeLongestPaths() {
        Stack<Integer> stack = new Stack<Integer>();

        // Mark all the vertices as not visited
        boolean visited[] = new boolean[V];
        for (int i = 0; i < V; i++)
            visited[i] = false;

        // Call the recursive helper function to store Topological
        // Sort starting from all vertices one by one
        for (int i = 0; i < V; i++)
            if (visited[i] == false)
                topologicalSortUtil(i, visited, stack);

        // Process vertices in topological order
        while (stack.isEmpty() == false) {

            // Get the next vertex from topological order
            int u = stack.peek();
            stack.pop();

            for (int l = 0; l < V; ++l) {
                dist[l][l] = 0;
                // Update distances of all adjacent vertices ;
                if (dist[l][u] != Integer.MIN_VALUE) {
                    for (int i = 0; i < adjs.get(u).size(); i++) {
                        Edge node = adjs.get(u).get(i);
                        if (dist[l][node.getTo()] < dist[l][u] + node.getWeight())
                            dist[l][node.getTo()] = dist[l][u] + node.getWeight();
                    }
                }
            }
        }
    }

    public int getLongestPath(int s, int d) {
        return dist[s][d];
    }

    @Override
    public String toString() {
        String s = "Costs:\n";
        for (int i = 0; i < V; i++) {
            for (int j = 0; j < V; j++) {
                if (dist[i][j] == Integer.MIN_VALUE)
                    s += "--\t";
                else
                    s += (dist[i][j] + "\t");
            }
            s += "\n";
        }

        return s;
    }

    // Each node of adjacency list contains vertex number of the vertex to which
    // edge connects and the weight.
    static class Edge {

        int to;
        int weight;

        Edge(int to, int weight) {
            this.to = to;
            this.weight = weight;
        }

        int getTo() {
            return to;
        }

        int getWeight() {
            return weight;
        }
    }
}
