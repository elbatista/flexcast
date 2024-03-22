package flexcast.reconfig.wlot;

import java.util.ArrayList;
import java.util.List;

public class DAG {
    private int id;
    private List<Integer> dag;
    public List<Integer> getDag() {
        return dag;
    }

    private List<Destination> destinations;
    private float totalCost;

    /**
     * Creates a new DAG with every possible destination
     * 
     * @param id  DAG identifier
     * @param dag the list of nodes in the DAG
     */
    public DAG(int id, List<Integer> dag) {
        this.id = id;
        this.dag = dag;
        this.totalCost = 0f;

        destinations = new ArrayList<>();
        // computeAllDests(dag, 0, new ArrayList<>());
    }

    /**
     * Creates a new DAG with the destinations in the provided workload
     * 
     * @param id  DAG identifier
     * @param dag the list of nodes in the DAG
     * @param wl  the workload with the destinations and use percentage
     */
    public DAG(int id, List<Integer> dag, Workload wl) {
        this.id = id;
        this.dag = dag;

        updateWorkload(wl);
    }

    /**
     * Recreates the destinations from the provided workload
     * 
     * @param wl the workload
     */
    public void updateWorkload(Workload wl) {
        destinations = new ArrayList<>();
        totalCost = 0;

        for (Destination d : wl.getDestinations()) {
            Destination tmp = d.clone();
            tmp.calculateCostNew(dag);
            destinations.add(tmp);
            totalCost += tmp.getPercentage() * tmp.getCost();
        }
    }

    public float getTotalCost() {
        return totalCost;
    }

    @Override
    public String toString() {
        String out = "DAG #" + id + " [";

        for (int e : dag) {
            out += e == dag.getLast() ? e + "]:\n" : e + " --> ";
        }
        for (Destination d : destinations) {
            out += d + "\n";
        }
        out += "Total cost = " + totalCost + "\n";
        return out;

    }

    @SuppressWarnings("unused")
    private void computeAllDests(List<Integer> l, int start, List<Integer> current) {
        if (current.size() > 1)
            destinations.add(new Destination(new ArrayList<>(current), this.dag));
        for (int i = start; i < l.size(); i++) {
            current.add(l.get(i));
            computeAllDests(l, i + 1, current);
            current.remove(current.size() - 1);
        }
    }
}
