package byzcast;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Random;
import java.util.concurrent.BrokenBarrierException;
import byzcast.messages.ByzCastMessage;
import util.ArgsParser;
import util.FileManager;
import util.Stats;

public class TpccByzCastClient extends ByzCastClient {

    private final Random gen;
    private int NUM_TX = 0;
    private int warehouseCount = 10;  // number of warehouses
    private int warehouseID = 0;  // client's main warehouse
    private int localityPercentage = 99;
    // Tpcc workload
    private static final int newOrderWeight = 45;
    private static final int paymentWeight = 43;
    private static final int orderStatusWeight = 4;
    private static final int deliveryWeight = 4;
    private static final int stockLevelWeight = 4;
    private HashMap<Short, String> nearestWHs = new HashMap<>();

    // enable for local-only workload
    boolean localOnly = false;
    double numNewOrderTx = 0;
    double numPaymentTx = 0;
    double numOrderStatusTx = 0;
    double numDeliveryTx = 0;
    double numStockLevelTx = 0;
    double multiPartitionTx = 0;
    double partitionsAccessedMultiPartitionTxs = 0;
    double partitionsAccessedAllTxs = 0;
    double numItemsAccessesNewOrder;

    int dest2NewOrder = 0;
    int dest2Payment = 0;
    int dest3 = 0;
    int dest4 = 0;
    int dest5 = 0;
    int dest6 = 0;
    int dest7 = 0;
    int dest8 = 0;
    int dest9 = 0;
    int dest10 = 0;
    int dest1 = 0;

    public TpccByzCastClient(short id, ArgsParser args) {
        super(id, args, false);
        this.gen = new Random(System.nanoTime());
        print("ByzCast TPC-C Client");
        FileManager.loadLocalityFile(nearestWHs);
        run();
    }
    
    public void run() {
        this.warehouseCount = numNodes;
        this.warehouseID = args.getHomeWarehouse();
        // wait all netty threads connect to all servers
        try {syncAllConnections.await();} catch(InterruptedException|BrokenBarrierException e){print("Broken barrier!!!!");}
        // send initialization message to all servers
        sendInitMessage();
        sleep(2000);
        // send ready message to a server
        // the server will reply when all clients are ready, then we "guarantee" all clients start at (~) the same time
        sendReadyMessage();
        print("All other clients ready!");

        print("Started ByzCast TPC-C experiment. Num nodes:", numNodes);
        print("ByzCast Tree:", args.getTree());
        print("My home warehouse:", warehouseID);

        print("Locality", args.getLocality(), "%");
        localityPercentage = args.getLocality();
        stats = new Stats(totalTime, numNodes);

        executeTransactions();

        if (stats.getCount() > 0) {
            try {Files.createDirectories(Paths.get("results" + (args.getRegion().equals("") ? "" : "/"+args.getRegion())));} catch (IOException e) {}
            stats.persist("results" + (args.getRegion().equals("") ? "" : "/"+args.getRegion()) + "/" + getId() + "-stats-tpcc-byzcast-client.txt", 10);
            print("LOCAL STATS:", stats);
        }

        sendEndMessage();

        System.out.println();
        System.out.println("Ratio of NewOrder Txs: " + numNewOrderTx / NUM_TX * 100 + "%");
        System.out.println("Ratio of Payment Txs: " + numPaymentTx / NUM_TX * 100 + "%");
        System.out.println("Ratio of OrderStatus Txs: " + numOrderStatusTx / NUM_TX * 100 + "%");
        System.out.println("Ratio of Delivery Txs: " + numDeliveryTx / NUM_TX * 100 + "%");
        System.out.println("Ratio of StockLevel Txs: " + numStockLevelTx / NUM_TX * 100 + "%");
        System.out.println();

        System.out.println("Multi-partition Txs: " + multiPartitionTx);
        System.out.println("# of partitions accessed (multi-partition Txs): " + partitionsAccessedMultiPartitionTxs);
        System.out.println("# of partitions accessed (all Txs): " + partitionsAccessedAllTxs);
        System.out.println();

        System.out.println("Ratio of multi-partition Txs: " + multiPartitionTx / NUM_TX * 100 + "%");
        System.out.println("Average # of partitions accessed (in multi-partition Txs): " + partitionsAccessedMultiPartitionTxs / multiPartitionTx);
        System.out.println("Average # of partitions accessed (in all Txs): " + partitionsAccessedAllTxs / NUM_TX);
        System.out.println();

        System.out.println("Average # of items accessed in NewOrder Txs: " + numItemsAccessesNewOrder / numNewOrderTx);
        System.out.println();

        System.out.println("# of Txs that targets 1 WH: " + dest1);
        System.out.println("# of Txs that targets 2 WH: " + (dest2NewOrder+dest2Payment) + " (New Order: " + dest2NewOrder + ", Payment: " + dest2Payment + ")");
        System.out.println("# of Txs that targets 3 WH: " + dest3);
        System.out.println("# of Txs that targets 4 WH: " + dest4);
        System.out.println("# of Txs that targets 5 WH: " + dest5);
        System.out.println("# of Txs that targets 6 WH: " + dest6);
        System.out.println("# of Txs that targets 7 WH: " + dest7);
        System.out.println("# of Txs that targets 8 WH: " + dest8);
        System.out.println("# of Txs that targets 9 WH: " + dest9);
        System.out.println("# of Txs that targets 10 WH: " + dest10);
        print();
        printWloadDistribution();
        exit();
    }
    
    private void executeTransactions() {

        long startTime = System.nanoTime(), now;
        long elapsed = 0, usLat = startTime;
        int totalMsgs=0;

        while (elapsed / 1e9 < totalTime) {
            int transactionType = randomNumber(1, 100, gen);
            int numDests = 1;

            if (transactionType <= newOrderWeight) {
                //transactionTypeName = "New-Order";
                numDests = doNewOrder();
            } else if (transactionType <= newOrderWeight + paymentWeight) {
                //transactionTypeName = "Payment";
                numDests = doPayment();
            } else if (transactionType <= newOrderWeight + paymentWeight + orderStatusWeight) {
                //transactionTypeName = "Order-Status";
                numDests = doOrderStatus();
            } else if (transactionType <= newOrderWeight + paymentWeight + orderStatusWeight + deliveryWeight) {
                //transactionTypeName = "Delivery";
                numDests = doDelivery();
            } else if (transactionType <= newOrderWeight + paymentWeight + orderStatusWeight + deliveryWeight + stockLevelWeight) {
                //transactionTypeName = "Stock-Level";
                numDests = doStockLevel();
            }

            ByzCastMessage m = newMessageTo(generateDests(numDests));

            multicast(m);
            computeDistribution(m);

            now = System.nanoTime();
            elapsed = (now - startTime);
            stats.store((now - usLat) / 1000, (numDests > 1));
            usLat = now;
            NUM_TX++;

            totalMsgs++;
            if(args.getNumMessages() > 0 && totalMsgs == args.getNumMessages()) break;
        }
        print("Finished ByzCast TPC-C experiment. Elapsed: ", elapsed / 1e9, "seconds");
    }

    private short[] generateDests(int numDests) {
        if(numDests == 1) return new short[]{(short)warehouseID};
        short [] tempdst = new short[numDests];
        if(numDests == 2) tempdst = generate2Dests();
        if(numDests == 3) tempdst = generate3Dests();
        if(numDests > 3) generateRandDests(tempdst, numDests);
        Arrays.sort(tempdst);
        return tempdst;
    }

    private void generateRandDests(short[] tempdst, int numDests) {
        ArrayList<Short> array = new ArrayList<>();
        array.add((short) warehouseID);
        tempdst[0] = (short) warehouseID;
        for(int i = 1; i < numDests; i++){
            short newDst;
            do {newDst = (short)randomNumber(0, (warehouseCount-1), gen);}
            while (array.contains(newDst));
            array.add(newDst);
            tempdst[i] = newDst;
        }
    }

    private short[] generate2Dests(){
        short [] tempdst = new short[2];
        tempdst[0] = (short) warehouseID;

        if(randomNumber(1, 100, gen) <= localityPercentage)
            tempdst[1] = getNearestWH(0);
        else 
            tempdst[1] = getNearestWH(1);

        Arrays.sort(tempdst);

        return tempdst;
    }

    private short[] generate3Dests(){
        short [] tempdst = new short[3];
        tempdst[0] = (short) warehouseID;
        if(randomNumber(1, 100, gen) <= localityPercentage){
            tempdst[1] = getNearestWH(0);
            tempdst[2] = getNearestWH(1);
        }else {
            tempdst[1] = getNearestWH(1);
            tempdst[2] = getNearestWH(2);
        }
        LinkedHashSet<Short> set = new LinkedHashSet<Short>();
 
        // remove duplicates
        for (short s : tempdst) set.add(s);
        short [] finaldst = new short[set.size()];
        int i = 0;
        for(short s : set){
            finaldst[i] = s;
            i++;
        }
        Arrays.sort(finaldst);

        return finaldst;
    }

    private short getNearestWH(int index) {
        short tempdst = -1;

        try{tempdst = Short.valueOf(nearestWHs.get((short)warehouseID).split(" ")[index].trim());} catch(Exception e){}

        if(tempdst == -1){
            // simply get the next HW in order of id
            tempdst = (short)(warehouseID+1);
            if(tempdst == warehouseCount) tempdst = (short)(warehouseID-1);
        }
        return tempdst;
    }

    public int doNewOrder() {
        int numItems = randomNumber(5, 15, gen);
        int[] supplierWarehouseIds = new int[numItems];

        for (int i = 0; i < numItems; i++) {
            if (localOnly){
                supplierWarehouseIds[i] = warehouseID;
            }
            else {
                if (randomNumber(1, 100, gen) > 1) {
                    supplierWarehouseIds[i] = warehouseID;
                } else {
                    do {
                        supplierWarehouseIds[i] = randomNumber(1, warehouseCount, gen);
                    }
                    while (supplierWarehouseIds[i] == warehouseID && warehouseCount > 1);
                }
            }
        }

        ArrayList<Integer> dest = new ArrayList<>();
        dest.add(warehouseID);
        for (int warehouseId : supplierWarehouseIds) {
            if (!dest.contains(warehouseId)) {
                dest.add(warehouseId);
            }
        }

        if (dest.size() == 2)
            dest2NewOrder++;
        if (dest.size() == 3)
            dest3++;
        if (dest.size() == 4)
            dest4++;
        if (dest.size() == 5)
            dest5++;
        if (dest.size() == 6)
            dest6++;
        if (dest.size() == 7)
            dest7++;
        if (dest.size() == 8)
            dest8++;
        if (dest.size() == 9)
            dest9++;
        if (dest.size() == 10)
            dest10++;
        if (dest.size() == 1)
            dest1++;

        numItemsAccessesNewOrder += numItems;
        numNewOrderTx++;
        if (dest.size() > 1) {
            multiPartitionTx++;
            partitionsAccessedMultiPartitionTxs += dest.size();
        }
        partitionsAccessedAllTxs += dest.size();
        return dest.size();
    }

    public int doPayment() {
        int x = randomNumber(1, 100, gen);
        int customerWarehouseID;

        if (localOnly) {
            customerWarehouseID = warehouseID;
        } else {
            if (x <= 85) {
                customerWarehouseID = warehouseID;
            } else {
                do {
                    customerWarehouseID = randomNumber(1, warehouseCount, gen);
                }
                while (customerWarehouseID == warehouseID && warehouseCount > 1);
            }
        }

        ArrayList<Integer> dest = new ArrayList<>();
        dest.add(warehouseID);
        if (!dest.contains(customerWarehouseID)) {
            dest.add(customerWarehouseID);
        }

        if (dest.size() == 1)
            dest1++;

        if (dest.size() == 2)
            dest2Payment++;

        numPaymentTx++;
        if (dest.size() > 1) {
            multiPartitionTx++;
            partitionsAccessedMultiPartitionTxs += dest.size();
        }
        partitionsAccessedAllTxs += dest.size();
        return dest.size();
    }

    public int doOrderStatus() {
        dest1++;
        numOrderStatusTx++;
        partitionsAccessedAllTxs += 1;
        return 1;
    }

    public int doDelivery() {
        dest1++;
        numDeliveryTx++;
        partitionsAccessedAllTxs += 1;
        return 1;
    }

    public int doStockLevel() {
        dest1++;
        numStockLevelTx++;
        partitionsAccessedAllTxs += 1;
        return 1;
    }

    public static int randomNumber(int min, int max, Random r) {
        return (int) (r.nextDouble() * (max - min + 1) + min);
    }

}
