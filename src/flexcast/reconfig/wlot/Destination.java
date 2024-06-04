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

    public Destination() {
        this(null);
    }

    public Destination(List<Integer> dst, float percentage) {
        this.destination = dst;
        this.percentage = percentage;
        this.cost = 0;
    }

    public Destination(List<Integer> dst, float percentage, List<Integer> dag) {
        this(dst, percentage);
        reorder(dag);
    }

    public Destination(List<Integer> dst) {
        this(dst, 0);
    }

    public List<Integer> getDestination() {
        return destination;
    }

    public void setDestination(List<Integer> dst) {
        this.destination = dst;
    }

    public float getPercentage() {
        return percentage;
    }
    
    public void setPercentage(float percentage) {
        this.percentage = percentage;
    }

    public float getCost() {
        return cost;
    }

    public void setCost(float cost) {
        this.cost = cost;
    }

    public void reorder(List<Integer> dag) {
        assert (dag.size() >= destination.size());
        destinationIdx = new ArrayList<>();
        for (int n : destination) {
            destinationIdx.add(dag.indexOf(n));
        }
        Collections.sort(destination,
                Comparator.comparing(item -> dag.indexOf(item)));
        Collections.sort(destinationIdx);
    }

    @Override
    public Destination clone() {
        Destination d = null;

        try {
            d = (Destination) super.clone();
            d.destination = new ArrayList<>(this.destination);
            d.percentage = this.percentage;
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
            out += d == destination.get(destination.size()-1) ? d : d + ",";
        }
        out += "}, Cost = " + cost;
        out += percentage == 0 ? "" : ", Perc = " + percentage;
        return out;
    }
}