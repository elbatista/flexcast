package flexcast.reconfig.wlot;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;
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
    private AtomicInteger dagId;
    private float minCost = Float.MAX_VALUE;
    private DAG minDAG;
    private ThreadPoolExecutor executor;
    private Lock lock;

    /**
     * Creates a DAG with size "graphsize" and no workload
     * 
     * @param graphSize the number of nodes in the created DAGs
     */
    public Wlot(int graphSize) {
        assert (graphSize <= 26);

        base = new ArrayList<>();
        for (int i = 0; i < graphSize; ++i) {
            base.add(firstNode + i);
        }
        wl = null;
        dagId = new AtomicInteger();
        executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        lock = new ReentrantLock();
    }

    /**
     * Calculates the cost for the workload "wl"
     * for every possible DAG with size "graphSize"
     * 
     * @param graphSize the number of nodes in the created DAGs
     * @param wl        the workload to evaluate
     */
    public Wlot(int graphSize, Workload wl) {
        this(graphSize);
        this.wl = wl;
        calculateMinCost(base);
    }

    /**
     * Obtains the DAG with the minimum cost for the provided workload
     * 
     * @return the DAG with minimum cost
     */
    public DAG getMinimumCostDAG() {
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

    private void calculateMinCost(List<Integer> base) {
        Collection<Future<?>> tasks = new LinkedList<Future<?>>();

        for (int first : base) {
            List<Integer> newBase = new ArrayList<>(base);
            newBase.remove(first);
            Future<?> f = executor.submit(() -> {
                permutation(Arrays.<Integer>asList(), first, newBase);
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

    private void permutation(List<Integer> prefix, Integer first, List<Integer> nums) {
        int n = nums.size();
        if (n == 0) {
            prefix.add(0, first);
            DAG dag = new DAG(dagId.getAndIncrement(), prefix, wl);
            // System.out.println("Thread " + Thread.currentThread().getName() + ": " +
            // prefix);
            lock.lock();
            try {
                if (dag.getTotalCost() < minCost) {
                    // System.out.println("Min DAG so far:" + dag);
                    minCost = dag.getTotalCost();
                    minDAG = dag;
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
                permutation(newPrefix, first, numsLeft);
            }
        }
    }

    @Override
    public String toString() {
        return "Min cost DAG (" + minCost + ")\n\t" + minDAG;
    }

    public static void main(String[] args) throws IOException {

        if (args.length < 2) {
            System.out.println("Missing graph size and workload file");
            System.exit(1);
        }

        // ObjectMapper mapper = new ObjectMapper();
        // Workload wl = new Workload(); //mapper.readValue(new FileInputStream(new File(args[1])), Workload.class);
        // System.out.println(wl);

        // Wlot wlot = new Wlot(Integer.parseInt(args[0]), wl);
        // DAG min = wlot.getMinimumCostDAG();
        // System.out.println("MIN " + min);
    }

}
