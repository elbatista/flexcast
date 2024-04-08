package flexcast.reconfig.wlot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class Workload {
    private List<Destination> destinations;

    /**
     * Creates a new empty workload
     * 
     */
    public Workload() {
        this(new ArrayList<>());
    }

    /**
     * Creates a new workload with a list of destinations
     * 
     * @param destinations the destinations (expected to also have the percentages)
     */
    public Workload(List<Destination> destinations) {
        this.destinations = destinations;
    }

    public Workload(HashMap<String, Integer> dstsFreq) {
        this();
        int total = 0;
        for(int v : dstsFreq.values()) total+=v;

        System.out.println("Total msgs: " + total);

        for(String k : dstsFreq.keySet()){
            float perc = ((float)100 * ((float)dstsFreq.get(k))) / (float)total;
            System.out.println(k+" - "+ dstsFreq.get(k) +"; total="+total+"; perc="+perc);
            Destination d = new Destination();
            d.setDestination(
                Arrays.asList(k.replace("[", "").replace("]", "").split(","))
                .stream().map(s -> {return Integer.parseInt(s.trim());}).collect(Collectors.toList())
            );
            d.setPercentage((float)perc/100);
            addDestination(d);
        }

        printDstsAsJSON();
    }

    private void printDstsAsJSON() {
        System.out.println("{\n\t\"workload\": [");
        for(Destination d : getDestinations()){
            System.out.println("\t\t{\n\t\t\t\"destination\": "+d.getDestination()+",");
            System.out.println("\t\t\t\"percentage\": "+d.getPercentage());
            System.out.println("\t\t},");
        }
        System.out.println("\t]\n}");
    }

    /**
     * The destinations for this workload
     * 
     */
    public List<Destination> getDestinations() {
        return destinations;
    }

    public void addDestination(Destination d) {
        this.destinations.add(d);
    }

    @Override
    public String toString() {
        String out = "Workload:\n";
        for (Destination d : destinations) {
            out += d + "\n";
        }

        return out;
    }
}
