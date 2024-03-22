package flexcast.reconfig.wlot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class Destination implements Cloneable {
    private List<Integer> destination;
    private List<Integer> destinationIdx;
    private float cost;
    private float percentage;

    public void setPercentage(float percentage) {
        this.percentage = percentage;
    }

    public Destination() {
        destination = new ArrayList<>();
        destinationIdx = new ArrayList<>();
        cost = 0;
        percentage = 0;
    }

    public Destination(List<Integer> dst, List<Integer> dag) {
        this(dst, 0f, dag);
    }

    public Destination(List<Integer> dst, float percentage, List<Integer> dag) {
        this.destination = dst;
        calculateCostNew(dag);
        this.percentage = percentage;
    }

    public Destination(List<Integer> dst) {
        this.destination = dst;
        this.cost = 0;
        this.percentage = 0;
    }

    public List<Integer> getDestination() {
        return destination;
    }

    public void setDestination(List<Integer> dst) {
        this.destination = dst;
    }

    public float getCost() {
        return cost;
    }

    public float getPercentage() {
        return percentage;
    }

    public void calculateCost(List<Integer> dag) {
        assert (dag.size() >= destination.size());
        destinationIdx = new ArrayList<>();
        for (int n : destination) {
            destinationIdx.add(dag.indexOf(n));
        }
        Collections.sort(destination,
                Comparator.comparing(item -> dag.indexOf(item)));
        Collections.sort(destinationIdx);
        this.cost = 0;
        for (int i = 0; i < destinationIdx.size() - 1; ++i) {
            cost += getCost(destinationIdx.get(i + 1) - destinationIdx.get(i) - 1, destinationIdx.get(i));
        }
    }

    public void calculateCostNew(List<Integer> dag) {
        cost = 0;
        destinationIdx = new ArrayList<>();
        for (int n : destination) {
            destinationIdx.add(dag.indexOf(n));
        }
        Collections.sort(destination,
                Comparator.comparing(item -> dag.indexOf(item)));
        Collections.sort(destinationIdx);
        // System.out.println("Destination.calculateCostNew()");
        // System.out.println(dag);
        // System.out.println(destination);
        int prevDest = 0;
        int posI = 0;
        int posD = 0;
        for (int i = destinationIdx.getFirst(); i <= destinationIdx.getLast(); ++i) {
            // lca
            // System.out.println("\nNode #" + i + ": " + dag.get(i));
            if (i == destinationIdx.getFirst()) {
                // System.out.println("LCA: cost = 1");
                ++cost;
                ++prevDest;
                ++posD;
            } else {
                // not lca
                if (destinationIdx.indexOf(i) > 0) { // not lca, dst node
                    ++posD;
                    ++prevDest;
                    int sumAcksInter = 0;

                    int posI2 = 0;
                    for (int ii = destinationIdx.getFirst(); ii < i; ++ii) {

                        // intermediaries only
                        if (destinationIdx.indexOf(ii) < 0) {
                            ++posI2;
                            sumAcksInter += (int) (Math.pow(2, (posI2 - 1)));
                        }
                    }

                    int c = 1 + // one data msg
                            posD - 2
                            + // acks from ancestors dests
                            sumAcksInter;
                    cost += c;

                    // System.out.println("NOT lca, dst node: posD = " + posD);
                    // System.out.println("NOT lca, dst node: sumackinter = " + sumAcksInter);
                    // System.out.println("NOT lca, dst node: cost = " + c);
                } else { // not lca, not dst node
                    ++posI;
                    int c = ((int) (Math.pow(2, posI - 1))) - 1 + prevDest;
                    cost += c;
                    // System.out.println("NOT lca, NOT dst node: cost = " + c);
                }
            }
        }
        // System.out.println("Final cost = " + cost);
    }

    private float getCost(int gaps, int height) {
        // number of gaps + 0.2 X height-of-the-gap
        return gaps == 0 ? 0 : gaps + 0.2f * height;
    }

    @Override
    protected Destination clone() {
        Destination d = null;

        try {
            d = (Destination) super.clone();
            d.destination = new ArrayList<>(this.destination);
            d.destinationIdx = new ArrayList<>(this.destinationIdx);
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        return d;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof Destination) {
            Destination d = (Destination) other;
            return d.getDestination().equals(this.getDestination());
        }
        return false;
    }

    @Override
    public String toString() {
        String out = "\tDestination {";

        for (int d : destination) {
            out += d == destination.getLast() ? d : d + ",";
        }
        out += "}, Cost = " + cost;
        out += percentage == 0 ? "" : ", Perc = " + percentage;
        return out;
    }
}