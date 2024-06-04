package flexcast.reconfig.wlot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
// import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Workload optimization tool
 * 
 * Calculates the best DAG for a provided workload
 */
public class Wlot {
    private final int firstNode = 0;
    private final List<Integer> base;
    private Workload wl;
    // private AtomicInteger dagId;
    private float minCost = Float.MAX_VALUE;
    private List<Integer> minDAG;
    private ThreadPoolExecutor executor;
    private Lock lock;
    private List<List<Integer>> weights;

    /**
     * Calculates the cost for the workload "wl"
     * for every possible DAG with size "graphSize"
     * 
     * @param graphSize the number of nodes in the created DAGs
     * @param wl        the workload to evaluate
     */
    public Wlot(List<List<Integer>> weights, Workload wl, boolean multithread) {
        int graphSize = weights.size();
        this.weights = weights;
        this.wl = wl;
        base = new ArrayList<>();
        for (int i = 0; i < graphSize; ++i) {
            base.add(firstNode + i);
        }
        // dagId = new AtomicInteger();
        executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        lock = new ReentrantLock();
        if (multithread)
            calculateMinCost(base);
        else
            calculateMinCostSingleThread(base);
    }

    /**
     * Obtains the DAG with the minimum cost for the provided workload
     * 
     * @return the DAG with minimum cost
     */
    public List<Integer> getMinimumCostDAG() {
        return minDAG;
    }

    /**
     * Updates the cost for the provided workload in each DAG
     * 
     * @param wl the workload to evaluate
     */
    public void updateWorkload(Workload wl) {
        this.wl = wl;
        calculateMinCost(base);
    }

    @SuppressWarnings("unused")
    private void calculateMinCostSingleThread(List<Integer> base) {
        for (int first : base) {
            List<Integer> newBase = new ArrayList<>(base);
            newBase.remove(first);
            permutation(Arrays.<Integer>asList(), first, newBase, wl);
        }
    }

    private void calculateMinCost(List<Integer> base) {
        Collection<Future<?>> tasks = new LinkedList<Future<?>>();

        for (int first : base) {
            List<Integer> newBase = new ArrayList<>(base);
            newBase.remove(first);
            Future<?> f = executor.submit(() -> {
                permutation(Arrays.<Integer>asList(), first, newBase, wl.clone());
            });
            tasks.add(f);
        }
        for (Future<?> t : tasks) {
            try {
                t.get();
            } catch (Exception e) {
                System.err.println("Error running task: " + e.getMessage());
            }
        }
        executor.shutdown();
    }

    private void permutation(List<Integer> prefix, Integer first, List<Integer> nums, Workload wl) {
        int n = nums.size();
        if (n == 0) {
            prefix.add(0, first);
            DAG dag = new DAG(prefix, weights);
            float cost = 0;
            for (Destination d : wl.getDestinations()) {
                d.reorder(prefix);
                d.setCost(dag.getLongestPath(d.getDestination().get(0),
                        d.getDestination().get(d.getDestination().size()-1)));
                cost += d.getCost() * d.getPercentage();
                // System.out.println(d);

            }
            // System.out.println("DAG # " + dagId.incrementAndGet() + ": " + prefix + " -
            // Total Cost = " + cost + "\n\n");
            lock.lock();
            try {
                if (cost < minCost) {
                    // System.out.println("Min DAG so far:" + dag);
                    minCost = cost;
                    minDAG = new ArrayList<>();
                    minDAG.addAll(prefix);
                    System.out.println("Min DAG so far:" + minDAG + "\n" + wl + dag);
                }
            } finally {
                lock.unlock();
            }
        } else {
            for (int i = 0; i < n; ++i) {
                List<Integer> newPrefix = new ArrayList<>();
                newPrefix.addAll(prefix);
                newPrefix.add(nums.get(i));
                List<Integer> numsLeft = new ArrayList<>();
                numsLeft.addAll(nums);
                numsLeft.remove(i);
                permutation(newPrefix, first, numsLeft, wl);
            }
        }
    }

    @Override
    public String toString() {
        return "Min cost DAG (" + minCost + ")\n\t" + minDAG;
    }
}
