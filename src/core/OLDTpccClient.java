package core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.BrokenBarrierException;
import java.util.stream.Collectors;
import messages.Message;
import util.ArgsParser;

public class OLDTpccClient extends Client {
    private final Random gen;
    private int warehouseCount = 6;  // number of warehouses
    private int warehouseID = 0;  // client's main warehouse
    private int NUM_TX = 0;

    // Tpcc workload
    private static final int newOrderWeight = 45;
    private static final int paymentWeight = 43;
    private static final int orderStatusWeight = 4;
    private static final int deliveryWeight = 4;
    private static final int stockLevelWeight = 4;

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
    int jump1, jump2, jump3, jump4;

    public OLDTpccClient(short id, ArgsParser args) {
        super(id, args, false);
        this.gen = new Random(System.nanoTime());
        this.warehouseCount = numNodes;
        print("TPCC Client");
        warehouseID = args.getHomeWarehouse();
        executeTransactions();
    }

    private void executeTransactions() {
        // wait all netty threads connect to all servers
        try {syncAllConnections.await();} catch(InterruptedException|BrokenBarrierException e){print("Broken barrier!!!!");}
        // send initialization message to all servers
        sendInitMessage();
        sleep(5000);
        // send ready message to a server
        // the server will reply when all clients are ready, then we "guarantee" all clients start at (~) the same time
        sendReadyMessage();
        print("All other clients ready!");

        print("Started tpcc experiment. Num nodes:", numNodes);
        print("My home warehouse:", warehouseID);
        if(args.getLocality() == 0) print("No locality");
        
        long startTime = System.nanoTime(), now;
        long elapsed = 0, usLat = startTime;

        while (elapsed / 1e9 < totalTime) {

            long transactionType = randomNumber(1, 100, gen);
            short[] dest = null;

            // dest = new short[]{(short)warehouseID}; // para testar soh locais, usar esta linha aqui e comentar as de baixo

            if (transactionType <= newOrderWeight) {
                dest = doNewOrder();
            } else if (transactionType <= newOrderWeight + paymentWeight) {
                dest = doPayment();
            } else if (transactionType <= newOrderWeight + paymentWeight + orderStatusWeight) {
                dest = doOrderStatus();
            } else if (transactionType <= newOrderWeight + paymentWeight + orderStatusWeight + deliveryWeight) {
                dest = doDelivery();
            } else if (transactionType <= newOrderWeight + paymentWeight + orderStatusWeight + deliveryWeight + stockLevelWeight) {
                dest = doStockLevel();
            }

            Message m = newMessageTo(dest);
            multicast(m);
            computeDistribution(m);

            now = System.nanoTime();
            elapsed = (now - startTime);
            stats.store((now - usLat) / 1000, (dest.length > 1));
            usLat = now;
            NUM_TX++;
        }

        if (stats.getCount() > 0) {
            try {Files.createDirectories(Paths.get("results" + (args.getRegion().equals("") ? "" : "/"+args.getRegion())));} catch (IOException e) {}
            stats.persist("results" + (args.getRegion().equals("") ? "" : "/"+args.getRegion()) + "/" + getId() + "-stats-tpcc-client.txt", 10);
            print("LOCAL STATS:", stats);
        }

        sendEndMessage();

        print("Finished tpcc experiment. Elapsed: ", elapsed / 1e9, "seconds");
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

        System.out.println("# of Txs Jump 1 WH: " + jump1);
        System.out.println("# of Txs Jump 2 WH: " + jump2);
        System.out.println("# of Txs Jump 3 WH: " + jump3);
        System.out.println("# of Txs Jump 4 WH: " + jump4);
        print();
        printWloadDistribution();
    }

    public short[] doNewOrder() {
        int numItems = randomNumber(5, 15, gen);
        int[] supplierWarehouseIds = new int[numItems];

        for (int i = 0; i < numItems; i++) {
            if(numNodes == 3){
                if(args.getLocality() == 0){
                    supplierWarehouseIds[i] = warehouseID;
                    if(randomNumber(1, 100, gen) > 95){
                        do{supplierWarehouseIds[i] = randomNumber(0, 2, gen);}while(supplierWarehouseIds[i]==warehouseID);
                    }
                }
                else {
                    calcWH3Nodes(supplierWarehouseIds, i);
                }
            }
            else if(numNodes == 6){
                if(args.getLocality() == 0){
                    supplierWarehouseIds[i] = warehouseID;
                    if(randomNumber(1, 100, gen) > 95){
                        do{supplierWarehouseIds[i] = randomNumber(0, 5, gen);}while(supplierWarehouseIds[i]==warehouseID);
                    }
                }
                else {
                    calcWH6Nodes(supplierWarehouseIds, i);
                }
            }
            else if(numNodes == 9){
                if(args.getLocality() == 0){
                    supplierWarehouseIds[i] = warehouseID;
                    if(randomNumber(1, 100, gen) > 95){
                        do{supplierWarehouseIds[i] = randomNumber(0, 8, gen);}while(supplierWarehouseIds[i]==warehouseID);
                    }
                }
                else {
                    calcWH9Nodes(supplierWarehouseIds, i);
                }
            }
            else if(numNodes == 12){
                if(args.getLocality() == 0){
                    supplierWarehouseIds[i] = warehouseID;
                    if(randomNumber(1, 100, gen) > 95){
                        do{supplierWarehouseIds[i] = randomNumber(0, 11, gen);}while(supplierWarehouseIds[i]==warehouseID);
                    }
                }
                else {
                    calcWH12Nodes(supplierWarehouseIds, i);
                }
            }
            else {
                supplierWarehouseIds[i] = warehouseID;
                if (randomNumber(1, 100, gen) <= args.getLocality()) {
                    supplierWarehouseIds[i] = warehouseID;
                }
                else {
                    int inc = 1; int dec = 1;
                    boolean incrementing = true;
                    do {
                        if(incrementing){
                            if(warehouseID+inc < warehouseCount){
                                supplierWarehouseIds[i] = warehouseID+inc;
                                inc++;
                            }
                            else {
                                incrementing = false;
                            }
                        }
                        else {
                            if(warehouseID-dec >= 0){
                                supplierWarehouseIds[i] = warehouseID-dec;
                                dec++;
                            }
                            else {
                                break;
                            }
                        }
                    }
                    while(randomNumber(1, 100, gen) > args.getLocality());
                }
            }
        }
        
        List<Integer> dest = new ArrayList<>();
        dest.add(warehouseID); 
        for (int warehouseId : supplierWarehouseIds) {
            if (!dest.contains(warehouseId)) {
                dest.add(warehouseId);
            }
        }

        short [] tempdst = new short[dest.size()];
        short i = 0;
        for(int u : dest.stream().sorted().collect(Collectors.toList())){
            tempdst[i] = (short)u;
            i++;
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
        return tempdst;
    }

    private void calcWH3Nodes(int[] supplierWarehouseIds, int i) {
        supplierWarehouseIds[i] = warehouseID;
        if(randomNumber(1, 100, gen) > args.getLocality()){
            if(warehouseID == 0){
                supplierWarehouseIds[i] = 1;
            }
            else if(warehouseID == 1){
                supplierWarehouseIds[i] = 0;
            }
            else if(warehouseID == 2){
                supplierWarehouseIds[i] = 1;
            }

            // jump one warehouse (low probability)
            if(randomNumber(1, 100, gen) > args.getLocality()){
                if(warehouseID == 0){
                    supplierWarehouseIds[i] = 2;
                }
                else if(warehouseID == 1){
                    supplierWarehouseIds[i] = 2;
                }
                else if(warehouseID == 2){
                    supplierWarehouseIds[i] = 0;
                }
            }
        }
    }

    private void calcWH6Nodes(int[] supplierWarehouseIds, int i) {
        supplierWarehouseIds[i] = warehouseID;
        if(randomNumber(1, 100, gen) > args.getLocality()){
            //clients in america
            if(warehouseID == 0){
                supplierWarehouseIds[i] = 1;
            }
            else if(warehouseID == 1){
                supplierWarehouseIds[i] = 0;
            }
            //clients in europe
            else if(warehouseID == 2){
                supplierWarehouseIds[i] = 3;
            }
            else if(warehouseID == 3){
                supplierWarehouseIds[i] = 2;
            }
            //clients in asia
            else if(warehouseID == 4){
                supplierWarehouseIds[i] = 5;
            }
            else if(warehouseID == 5){
                supplierWarehouseIds[i] = 4;
            }

            // jump one warehouse (low probability)
            if(randomNumber(1, 100, gen) > args.getLocality()){
                jump1++;
                //clients in america
                if(warehouseID == 0){
                    supplierWarehouseIds[i] = 2;
                }
                else if(warehouseID == 1){
                    supplierWarehouseIds[i] = 3;
                }
                //clients in europe
                else if(warehouseID == 2){
                    supplierWarehouseIds[i] = 0;
                }
                else if(warehouseID == 3){
                    supplierWarehouseIds[i] = 1;
                }
                //clients in asia
                else if(warehouseID == 4){
                    supplierWarehouseIds[i] = 2;
                }
                else if(warehouseID == 5){
                    supplierWarehouseIds[i] = 3;
                }

                // jump two warehouse (low probability)
                if(randomNumber(1, 100, gen) > args.getLocality()){
                    jump1--; jump2++;
                    //warehouse in california
                    if(warehouseID == 0){
                        //nearest warehouse jump2 is fraknfurt
                        supplierWarehouseIds[i] = 3;
                    }
                    //warehouse in virginia
                    else if(warehouseID == 1){
                        supplierWarehouseIds[i] = 4;
                    }
                    //clients in europe
                    else if(warehouseID == 2){
                        supplierWarehouseIds[i] = 5;
                    }
                    else if(warehouseID == 3){
                        supplierWarehouseIds[i] = 0;
                    }
                    //clients in asia
                    else if(warehouseID == 4){
                        supplierWarehouseIds[i] = 1;
                    }
                    else if(warehouseID == 5){
                        supplierWarehouseIds[i] = 2;
                    }

                    // jump 3 warehouse (low probability)
                    if(randomNumber(1, 100, gen) > args.getLocality()){
                        jump2--; jump3++;
                        //warehouse in california
                        if(warehouseID == 0){
                            //nearest warehouse jump2 is fraknfurt
                            supplierWarehouseIds[i] = 4;
                        }
                        //warehouse in virginia
                        else if(warehouseID == 1){
                            supplierWarehouseIds[i] = 5;
                        }
                        //clients in europe
                        else if(warehouseID == 2){
                            supplierWarehouseIds[i] = 5;
                        }
                        else if(warehouseID == 3){
                            supplierWarehouseIds[i] = 0;
                        }
                        //clients in asia
                        else if(warehouseID == 4){
                            supplierWarehouseIds[i] = 0;
                        }
                        else if(warehouseID == 5){
                            supplierWarehouseIds[i] = 1;
                        }

                        // jump 4 warehouse (low probability)
                        if(randomNumber(1, 100, gen) > args.getLocality()){
                            jump3--; jump4++;
                            //warehouse in california
                            if(warehouseID == 0){
                                //nearest warehouse jump2 is fraknfurt
                                supplierWarehouseIds[i] = 5;
                            }
                            else if(warehouseID == 5){
                                supplierWarehouseIds[i] = 0;
                            }
                        }
                    }
                }
            }
        }
    }

    private void calcWH9Nodes(int[] supplierWarehouseIds, int i) {
        supplierWarehouseIds[i] = warehouseID;
        if(randomNumber(1, 100, gen) > args.getLocality()){
            //warehouse in california
            if(warehouseID == 0){
                //nearest warehouse is virginia
                supplierWarehouseIds[i] = 1;
            }
            //warehouse in virginia
            else if(warehouseID == 1){
                //nearest warehouse is california
                supplierWarehouseIds[i] = 0;
            }
            //warehouse in sao paulo
            else if(warehouseID == 2){
                //nearest warehouse is virginia
                supplierWarehouseIds[i] = 1;
            }
            //warehouse in ireland
            else if(warehouseID == 3){
                //nearest warehouse is frankfurt
                supplierWarehouseIds[i] = 4;
            }
            //warehouse in frankfurt
            else if(warehouseID == 4){
                //nearest warehouse is ireland
                supplierWarehouseIds[i] = 3;
            }
            //warehouse in stockholm
            else if(warehouseID == 5){
                //nearest warehouse is frankfurt
                supplierWarehouseIds[i] = 4;
            }
            //warehouse in mumbai
            else if(warehouseID == 6){
                //nearest warehouse is singapore
                supplierWarehouseIds[i] = 7;
            }
            //warehouse in singapore
            else if(warehouseID == 7){
                //nearest warehouse is mumbai
                supplierWarehouseIds[i] = 6;
            }
            //warehouse in tokyo
            else if(warehouseID == 8){
                //nearest warehouse is singapore
                supplierWarehouseIds[i] = 7;
            }

            // jump one warehouse (low probability)
            if(randomNumber(1, 100, gen) > args.getLocality()){
                jump1++;
                //warehouse in california
                if(warehouseID == 0){
                    //nearest warehouse jump is sao paulo
                    supplierWarehouseIds[i] = 2;
                }
                //warehouse in virginia
                else if(warehouseID == 1){
                    //nearest warehouse jump is ireland
                    supplierWarehouseIds[i] = 3;
                }
                //warehouse in sao paulo
                else if(warehouseID == 2){
                    //nearest warehouse jump is california
                    supplierWarehouseIds[i] = 0;
                }
                //warehouse in ireland
                else if(warehouseID == 3){
                    //nearest warehouse jump is stockholm
                    supplierWarehouseIds[i] = 5;
                }
                //warehouse in frankfurt
                else if(warehouseID == 4){
                    //nearest warehouse jump is mumbai
                    supplierWarehouseIds[i] = 6;
                }
                //warehouse in stockholm
                else if(warehouseID == 5){
                    //nearest warehouse jump is ireland
                    supplierWarehouseIds[i] = 3;
                }
                //warehouse in mumbai
                else if(warehouseID == 6){
                    //nearest warehouse jump is tokyo
                    supplierWarehouseIds[i] = 8;
                }
                //warehouse in singapore
                else if(warehouseID == 7){
                    //nearest warehouse jump is stockholm
                    supplierWarehouseIds[i] = 5;
                }
                //warehouse in tokyo
                else if(warehouseID == 8){
                    //nearest warehouse jump is mumbai
                    supplierWarehouseIds[i] = 6;
                }

                // jump 2 warehouse (low probability)
                if(randomNumber(1, 100, gen) > args.getLocality()){
                    jump1--; jump2++;
                    //warehouse in california
                    if(warehouseID == 0){
                        supplierWarehouseIds[i] = 3;
                    }
                    //warehouse in virginia
                    else if(warehouseID == 1){
                        supplierWarehouseIds[i] = 4;
                    }
                    //warehouse in sao paulo
                    else if(warehouseID == 2){
                        supplierWarehouseIds[i] = 5;
                    }
                    //warehouse in ireland
                    else if(warehouseID == 3){
                        supplierWarehouseIds[i] = 0;
                    }
                    //warehouse in frankfurt
                    else if(warehouseID == 4){
                        supplierWarehouseIds[i] = 1;
                    }
                    //warehouse in stockholm
                    else if(warehouseID == 5){
                        supplierWarehouseIds[i] = 2;
                    }
                    //warehouse in mumbai
                    else if(warehouseID == 6){
                        supplierWarehouseIds[i] = 3;
                    }
                    //warehouse in singapore
                    else if(warehouseID == 7){
                        //nearest warehouse jump is stockholm
                        supplierWarehouseIds[i] = 4;
                    }
                    //warehouse in tokyo
                    else if(warehouseID == 8){
                        //nearest warehouse jump is mumbai
                        supplierWarehouseIds[i] = 5;
                    }

                    // jump 3 warehouse (low probability)
                    if(randomNumber(1, 100, gen) > args.getLocality()){
                        jump2--; jump3++;
                        //warehouse in california
                        if(warehouseID == 0){
                            supplierWarehouseIds[i] = 4;
                        }
                        //warehouse in virginia
                        else if(warehouseID == 1){
                            supplierWarehouseIds[i] = 5;
                        }
                        //warehouse in sao paulo
                        else if(warehouseID == 2){
                            supplierWarehouseIds[i] = 6;
                        }
                        //warehouse in ireland
                        else if(warehouseID == 3){
                            supplierWarehouseIds[i] = 7;
                        }
                        //warehouse in frankfurt
                        else if(warehouseID == 4){
                            supplierWarehouseIds[i] = 0;
                        }
                        //warehouse in stockholm
                        else if(warehouseID == 5){
                            supplierWarehouseIds[i] = 1;
                        }
                        //warehouse in mumbai
                        else if(warehouseID == 6){
                            supplierWarehouseIds[i] = 2;
                        }
                        //warehouse in singapore
                        else if(warehouseID == 7){
                            //nearest warehouse jump is stockholm
                            supplierWarehouseIds[i] = 3;
                        }
                        //warehouse in tokyo
                        else if(warehouseID == 8){
                            //nearest warehouse jump is mumbai
                            supplierWarehouseIds[i] = 4;
                        }

                        // jump 4 warehouse (low probability)
                        if(randomNumber(1, 100, gen) > args.getLocality()){
                            jump3--; jump4++;
                            if(warehouseID == 0){
                                supplierWarehouseIds[i] = 5;
                            }
                            else if(warehouseID == 1){
                                supplierWarehouseIds[i] = 6;
                            }
                            else if(warehouseID == 2){
                                supplierWarehouseIds[i] = 7;
                            }
                            else if(warehouseID == 3){
                                supplierWarehouseIds[i] = 8;
                            } else if(warehouseID == 8){
                                supplierWarehouseIds[i] = 3;
                            }
                            else if(warehouseID == 7){
                                supplierWarehouseIds[i] = 2;
                            }
                            else if(warehouseID == 6){
                                supplierWarehouseIds[i] = 1;
                            }
                            else if(warehouseID == 5){
                                supplierWarehouseIds[i] = 0;
                            }
                        }
                    }
                }
            }
        }
    }

    private void calcWH12Nodes(int[] supplierWarehouseIds, int i) {
        supplierWarehouseIds[i] = warehouseID;
        if(randomNumber(1, 100, gen) > args.getLocality()){
            //warehouse in california
            if(warehouseID == 0){
                //nearest warehouse is canada
                supplierWarehouseIds[i] = 1;
            }
            //warehouse in canada
            else if(warehouseID == 1){
                //nearest warehouse is virginia
                supplierWarehouseIds[i] = 2;
            }
            //warehouse in virginia
            else if(warehouseID == 2){
                //nearest warehouse is canada
                supplierWarehouseIds[i] = 1;
            }
            //warehouse in sao paulo
            else if(warehouseID == 3){
                //nearest warehouse is virginia
                supplierWarehouseIds[i] = 2;
            }
            //warehouse in ireland
            else if(warehouseID == 4){
                //nearest warehouse is paris
                supplierWarehouseIds[i] = 5;
            }
            //warehouse in paris
            else if(warehouseID == 5){
                //nearest warehouse is ireland
                supplierWarehouseIds[i] = 4;
            }
            //warehouse in frankfurt
            else if(warehouseID == 6){
                //nearest warehouse is stockholm
                supplierWarehouseIds[i] = 7;
            }
            //warehouse in stockholm
            else if(warehouseID == 7){
                //nearest warehouse is frankfurt
                supplierWarehouseIds[i] = 6;
            }
            //warehouse in mumbai
            else if(warehouseID == 8){
                //nearest warehouse is singapore
                supplierWarehouseIds[i] = 9;
            }
            //warehouse in singapore
            else if(warehouseID == 9){
                //nearest warehouse is mumbai
                supplierWarehouseIds[i] = 8;
            }
            //warehouse in tokyo
            else if(warehouseID == 10){
                //nearest warehouse is sydney
                supplierWarehouseIds[i] = 11;
            }
            //warehouse in sydney
            else if(warehouseID == 11){
                //nearest warehouse is tokyo
                supplierWarehouseIds[i] = 10;
            }

            // jump one warehouse (low probability)
            if(randomNumber(1, 100, gen) > args.getLocality()){
                jump1++;
                //warehouse in california
                if(warehouseID == 0){
                    //nearest warehouse jump is virginia
                    supplierWarehouseIds[i] = 2;
                }
                //warehouse in canada
                else if(warehouseID == 1){
                    //nearest warehouse jump is sao paulo
                    supplierWarehouseIds[i] = 3;
                }
                //warehouse in virginia
                else if(warehouseID == 2){
                    //nearest warehouse jump is california
                    supplierWarehouseIds[i] = 0;
                }
                //warehouse in sao paulo
                else if(warehouseID == 3){
                    //nearest warehouse jump is canada
                    supplierWarehouseIds[i] = 1;
                }
                //warehouse in ireland
                else if(warehouseID == 4){
                    //nearest warehouse jump is frankfurt
                    supplierWarehouseIds[i] = 6;
                }
                //warehouse in paris
                else if(warehouseID == 5){
                    //nearest warehouse jump is stockholm
                    supplierWarehouseIds[i] = 7;
                }
                //warehouse in frankfurt
                else if(warehouseID == 6){
                    //nearest warehouse jump is ireland
                    supplierWarehouseIds[i] = 4;
                }
                //warehouse in stockholm
                else if(warehouseID == 7){
                    //nearest warehouse jump is paris
                    supplierWarehouseIds[i] = 5;
                }
                //warehouse in mumbai
                else if(warehouseID == 8){
                    //nearest warehouse jump is tokyo
                    supplierWarehouseIds[i] = 10;
                }
                //warehouse in singapore
                else if(warehouseID == 9){
                    //nearest warehouse jump is sydney
                    supplierWarehouseIds[i] = 11;
                }
                //warehouse in tokyo
                else if(warehouseID == 10){
                    //nearest warehouse jump is mumbai
                    supplierWarehouseIds[i] = 8;
                }
                //warehouse in sydney
                else if(warehouseID == 11){
                    //nearest warehouse jump is singapore
                    supplierWarehouseIds[i] = 9;
                }

                // jump 2 warehouse (very low probability)
                if(randomNumber(1, 100, gen) > args.getLocality()){
                    jump1--;jump2++;
                    //warehouse in california
                    if(warehouseID == 0){
                        //nearest warehouse jump2 is sao paulo
                        supplierWarehouseIds[i] = 3;
                    }
                    //warehouse in canada
                    else if(warehouseID == 1){
                        //nearest warehouse jump2 is ireland
                        supplierWarehouseIds[i] = 4;
                    }
                    //warehouse in virginia
                    else if(warehouseID == 2){
                        //nearest warehouse jump2 is paris
                        supplierWarehouseIds[i] = 5;
                    }
                    //warehouse in sao paulo
                    else if(warehouseID == 3){
                        //nearest warehouse jump2 is california
                        supplierWarehouseIds[i] = 0;
                    }
                    //warehouse in ireland
                    else if(warehouseID == 4){
                        //nearest warehouse jump2 is stockholm
                        supplierWarehouseIds[i] = 7;
                    }
                    //warehouse in paris
                    else if(warehouseID == 5){
                        //nearest warehouse jump2 is virginia
                        supplierWarehouseIds[i] = 2;
                    }
                    //warehouse in frankfurt
                    else if(warehouseID == 6){
                        //nearest warehouse jump2 is singapore
                        supplierWarehouseIds[i] = 9;
                    }
                    //warehouse in stockholm
                    else if(warehouseID == 7){
                        //nearest warehouse jump is ireland
                        supplierWarehouseIds[i] = 4;
                    }
                    //warehouse in mumbai
                    else if(warehouseID == 8){
                        //nearest warehouse jump2 is sydney
                        supplierWarehouseIds[i] = 11;
                    }
                    //warehouse in singapore
                    else if(warehouseID == 9){
                        //nearest warehouse jump2 is frankfurt
                        supplierWarehouseIds[i] = 6;
                    }
                    //warehouse in tokyo
                    else if(warehouseID == 10){
                        //nearest warehouse jump2 is stockholm
                        supplierWarehouseIds[i] = 7;
                    }
                    //warehouse in sydney
                    else if(warehouseID == 11){
                        //nearest warehouse jump2 is mumbai
                        supplierWarehouseIds[i] = 8;
                    }

                    // jump 3 warehouse (very low probability)
                    if(randomNumber(1, 100, gen) > args.getLocality()){
                        jump2--;jump3++;
                        if(warehouseID == 0){
                            supplierWarehouseIds[i] = 4;
                        }
                        else if(warehouseID == 1){
                            supplierWarehouseIds[i] = 5;
                        }
                        else if(warehouseID == 2){
                            supplierWarehouseIds[i] = 6;
                        }
                        else if(warehouseID == 3){
                            supplierWarehouseIds[i] = 7;
                        }
                        else if(warehouseID == 4){
                            supplierWarehouseIds[i] = 7;
                        }
                        else if(warehouseID == 5){
                            supplierWarehouseIds[i] = 1;
                        }
                        else if(warehouseID == 6){
                            supplierWarehouseIds[i] = 2;
                        }
                        else if(warehouseID == 7){
                            supplierWarehouseIds[i] = 3;
                        }
                        else if(warehouseID == 8){
                            supplierWarehouseIds[i] = 4;
                        }
                        else if(warehouseID == 9){
                            supplierWarehouseIds[i] = 5;
                        }
                        else if(warehouseID == 10){
                            supplierWarehouseIds[i] = 6;
                        }
                        else if(warehouseID == 11){
                            supplierWarehouseIds[i] = 7;
                        }

                        // jump 4 warehouse (very low probability)
                        if(randomNumber(1, 100, gen) > args.getLocality()){
                            jump3--;jump4++;
                            if(warehouseID == 0){
                                supplierWarehouseIds[i] = 5;
                            }
                            else if(warehouseID == 1){
                                supplierWarehouseIds[i] = 6;
                            }
                            else if(warehouseID == 2){
                                supplierWarehouseIds[i] = 7;
                            }
                            else if(warehouseID == 3){
                                supplierWarehouseIds[i] = 8;
                            }
                            else if(warehouseID == 4){
                                supplierWarehouseIds[i] = 9;
                            }
                            else if(warehouseID == 5){
                                supplierWarehouseIds[i] = 0;
                            }
                            else if(warehouseID == 6){
                                supplierWarehouseIds[i] = 1;
                            }
                            else if(warehouseID == 7){
                                supplierWarehouseIds[i] = 2;
                            }
                            else if(warehouseID == 8){
                                supplierWarehouseIds[i] = 3;
                            }
                            else if(warehouseID == 9){
                                supplierWarehouseIds[i] = 4;
                            }
                            else if(warehouseID == 10){
                                supplierWarehouseIds[i] = 5;
                            }
                            else if(warehouseID == 11){
                                supplierWarehouseIds[i] = 6;
                            }
                        }
                    }
                }
            }
        }
    }

    public short[] doPayment() {
        int customerWarehouseID=warehouseID;

        if(numNodes == 3){
            customerWarehouseID=warehouseID;
            if(randomNumber(1, 100, gen) > args.getLocality()){
                if(warehouseID == 0){
                    customerWarehouseID = 1;
                }
                else if(warehouseID == 1){
                    customerWarehouseID = 0;
                }
                else if(warehouseID == 2){
                    customerWarehouseID = 1;
                }
                // jump one warehouse (low probability)
                if(randomNumber(1, 100, gen) > args.getLocality()){
                    jump1++;
                    if(warehouseID == 0){
                        customerWarehouseID = 2;
                    }
                    else if(warehouseID == 1){
                        customerWarehouseID = 2;
                    }
                    else if(warehouseID == 2){
                        customerWarehouseID = 0;
                    }
                }
            }
        }
        else if(numNodes == 6){
            if(args.getLocality() == 0){
                customerWarehouseID = warehouseID;
                if(randomNumber(1, 100, gen) > 95){
                    do{customerWarehouseID = randomNumber(0, 5, gen);}while(customerWarehouseID==warehouseID);
                }
            }
            else {
                if(randomNumber(1, 100, gen) > args.getLocality()){
                    if(warehouseID == 0){
                        customerWarehouseID = 1;
                    }
                    else if(warehouseID == 1){
                        customerWarehouseID = 0;
                    }
                    else if(warehouseID == 2){
                        customerWarehouseID = 3;
                    }
                    else if(warehouseID == 3){
                        customerWarehouseID = 2;
                    }
                    else if(warehouseID == 4){
                        customerWarehouseID = 5;
                    }
                    else if(warehouseID == 5){
                        customerWarehouseID = 4;
                    }
        
                    // jump one warehouse (low probability)
                    if(randomNumber(1, 100, gen) > args.getLocality()){
                        jump1++;
                        if(warehouseID == 0){
                            customerWarehouseID = 2;
                        }
                        else if(warehouseID == 1){
                            customerWarehouseID = 3;
                        }
                        else if(warehouseID == 2){
                            customerWarehouseID = 0;
                        }
                        else if(warehouseID == 3){
                            customerWarehouseID = 1;
                        }
                        else if(warehouseID == 4){
                            customerWarehouseID = 2;
                        }
                        else if(warehouseID == 5){
                            customerWarehouseID = 3;
                        }

                        // jump 2 warehouse (low probability)
                        if(randomNumber(1, 100, gen) > args.getLocality()){
                            jump1--; jump2++;
                            if(warehouseID == 0){
                                customerWarehouseID = 3;
                            }
                            else if(warehouseID == 1){
                                customerWarehouseID = 4;
                            }
                            else if(warehouseID == 2){
                                customerWarehouseID = 5;
                            }
                            else if(warehouseID == 3){
                                customerWarehouseID = 0;
                            }
                            else if(warehouseID == 4){
                                customerWarehouseID = 1;
                            }
                            else if(warehouseID == 5){
                                customerWarehouseID = 2;
                            }

                            // jump 3 warehouse (low probability)
                            if(randomNumber(1, 100, gen) > args.getLocality()){
                                jump2--; jump3++;
                                if(warehouseID == 0){
                                    customerWarehouseID = 4;
                                }
                                else if(warehouseID == 1){
                                    customerWarehouseID = 5;
                                }
                                else if(warehouseID == 2){
                                    customerWarehouseID = 0;
                                }
                                else if(warehouseID == 3){
                                    customerWarehouseID = 0;
                                }
                                else if(warehouseID == 4){
                                    customerWarehouseID = 0;
                                }
                                else if(warehouseID == 5){
                                    customerWarehouseID = 1;
                                }

                                // jump 4 warehouse (low probability)
                                if(randomNumber(1, 100, gen) > args.getLocality()){
                                    jump3--; jump4++;
                                    if(warehouseID == 0){
                                        customerWarehouseID = 5;
                                    }
                                    else if(warehouseID == 1){
                                        customerWarehouseID = 5;
                                    }
                                    else if(warehouseID == 2){
                                        customerWarehouseID = 0;
                                    }
                                    else if(warehouseID == 3){
                                        customerWarehouseID = 0;
                                    }
                                    else if(warehouseID == 4){
                                        customerWarehouseID = 0;
                                    }
                                    else if(warehouseID == 5){
                                        customerWarehouseID = 0;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        else if(numNodes == 9){
            if(args.getLocality() == 0){
                customerWarehouseID = warehouseID;
                if(randomNumber(1, 100, gen) > 95){
                    do{customerWarehouseID = randomNumber(0, 8, gen);}while(customerWarehouseID==warehouseID);
                }
            }
            else {
                customerWarehouseID = warehouseID;
                if(randomNumber(1, 100, gen) > args.getLocality()){
                    //warehouse in california
                    if(warehouseID == 0){
                        //nearest warehouse is virginia
                        customerWarehouseID = 1;
                    }
                    //warehouse in virginia
                    else if(warehouseID == 1){
                        //nearest warehouse is california
                        customerWarehouseID = 0;
                    }
                    //warehouse in sao paulo
                    else if(warehouseID == 2){
                        //nearest warehouse is virginia
                        customerWarehouseID = 1;
                    }
                    //warehouse in ireland
                    else if(warehouseID == 3){
                        //nearest warehouse is frankfurt
                        customerWarehouseID = 4;
                    }
                    //warehouse in frankfurt
                    else if(warehouseID == 4){
                        //nearest warehouse is ireland
                        customerWarehouseID = 3;
                    }
                    //warehouse in stockholm
                    else if(warehouseID == 5){
                        //nearest warehouse is frankfurt
                        customerWarehouseID = 4;
                    }
                    //warehouse in mumbai
                    else if(warehouseID == 6){
                        //nearest warehouse is singapore
                        customerWarehouseID = 7;
                    }
                    //warehouse in singapore
                    else if(warehouseID == 7){
                        //nearest warehouse is mumbai
                        customerWarehouseID = 6;
                    }
                    //warehouse in tokyo
                    else if(warehouseID == 8){
                        //nearest warehouse is singapore
                        customerWarehouseID = 7;
                    }

                    // jump one warehouse (low probability)
                    if(randomNumber(1, 100, gen) > args.getLocality()){
                        jump1++;
                        //warehouse in california
                        if(warehouseID == 0){
                            //nearest warehouse jump is sao paulo
                            customerWarehouseID = 2;
                        }
                        //warehouse in virginia
                        else if(warehouseID == 1){
                            //nearest warehouse jump is ireland
                            customerWarehouseID = 3;
                        }
                        //warehouse in sao paulo
                        else if(warehouseID == 2){
                            //nearest warehouse jump is california
                            customerWarehouseID = 0;
                        }
                        //warehouse in ireland
                        else if(warehouseID == 3){
                            //nearest warehouse jump is stockholm
                            customerWarehouseID = 5;
                        }
                        //warehouse in frankfurt
                        else if(warehouseID == 4){
                            //nearest warehouse jump is mumbai
                            customerWarehouseID = 6;
                        }
                        //warehouse in stockholm
                        else if(warehouseID == 5){
                            //nearest warehouse jump is ireland
                            customerWarehouseID = 3;
                        }
                        //warehouse in mumbai
                        else if(warehouseID == 6){
                            //nearest warehouse jump is tokyo
                            customerWarehouseID = 8;
                        }
                        //warehouse in singapore
                        else if(warehouseID == 7){
                            //nearest warehouse jump is stockholm
                            customerWarehouseID = 5;
                        }
                        //warehouse in tokyo
                        else if(warehouseID == 8){
                            //nearest warehouse jump is mumbai
                            customerWarehouseID = 6;
                        }

                        // jump 2 warehouse (low probability)
                        if(randomNumber(1, 100, gen) > args.getLocality()){
                            jump1--; jump2++;
                            if(warehouseID == 0){
                                customerWarehouseID = 3;
                            }
                            else if(warehouseID == 1){
                                customerWarehouseID = 4;
                            }
                            else if(warehouseID == 2){
                                customerWarehouseID = 5;
                            }
                            else if(warehouseID == 3){
                                customerWarehouseID = 0;
                            }
                            else if(warehouseID == 4){
                                customerWarehouseID = 1;
                            }
                            else if(warehouseID == 5){
                                customerWarehouseID = 2;
                            }
                            else if(warehouseID == 6){
                                customerWarehouseID = 3;
                            }
                            else if(warehouseID == 7){
                                customerWarehouseID = 4;
                            }
                            else if(warehouseID == 8){
                                customerWarehouseID = 5;
                            }

                            // jump 3 warehouse (low probability)
                            if(randomNumber(1, 100, gen) > args.getLocality()){
                                jump2--; jump3++;
                                if(warehouseID == 0){
                                    customerWarehouseID = 5;
                                }
                                else if(warehouseID == 1){
                                    customerWarehouseID = 6;
                                }
                                else if(warehouseID == 2){
                                    customerWarehouseID = 7;
                                }
                                else if(warehouseID == 3){
                                    customerWarehouseID = 8;
                                }
                                else if(warehouseID == 4){
                                    customerWarehouseID = 0;
                                }
                                else if(warehouseID == 5){
                                    customerWarehouseID = 1;
                                }
                                else if(warehouseID == 6){
                                    customerWarehouseID = 2;
                                }
                                else if(warehouseID == 7){
                                    customerWarehouseID = 3;
                                }
                                else if(warehouseID == 8){
                                    customerWarehouseID = 4;
                                }

                                // jump 4 warehouse (low probability)
                                if(randomNumber(1, 100, gen) > args.getLocality()){
                                    jump3--; jump4++;
                                    if(warehouseID == 0){
                                        customerWarehouseID = 6;
                                    }
                                    else if(warehouseID == 1){
                                        customerWarehouseID = 7;
                                    }
                                    else if(warehouseID == 2){
                                        customerWarehouseID = 8;
                                    }
                                    else if(warehouseID == 3){
                                        customerWarehouseID = 0;
                                    }
                                    else if(warehouseID == 4){
                                        customerWarehouseID = 0;
                                    }
                                    else if(warehouseID == 5){
                                        customerWarehouseID = 0;
                                    }
                                    else if(warehouseID == 6){
                                        customerWarehouseID = 1;
                                    }
                                    else if(warehouseID == 7){
                                        customerWarehouseID = 2;
                                    }
                                    else if(warehouseID == 8){
                                        customerWarehouseID = 3;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        else if(numNodes == 12) {
            customerWarehouseID = warehouseID;
            if(randomNumber(1, 100, gen) > args.getLocality()){
                //warehouse in california
                if(warehouseID == 0){
                    //nearest warehouse is canada
                    customerWarehouseID = 1;
                }
                //warehouse in canada
                else if(warehouseID == 1){
                    //nearest warehouse is virginia
                    customerWarehouseID = 2;
                }
                //warehouse in virginia
                else if(warehouseID == 2){
                    //nearest warehouse is canada
                    customerWarehouseID = 1;
                }
                //warehouse in sao paulo
                else if(warehouseID == 3){
                    //nearest warehouse is virginia
                    customerWarehouseID = 2;
                }
                //warehouse in ireland
                else if(warehouseID == 4){
                    //nearest warehouse is paris
                    customerWarehouseID = 5;
                }
                //warehouse in paris
                else if(warehouseID == 5){
                    //nearest warehouse is ireland
                    customerWarehouseID = 4;
                }
                //warehouse in frankfurt
                else if(warehouseID == 6){
                    //nearest warehouse is stockholm
                    customerWarehouseID = 7;
                }
                //warehouse in stockholm
                else if(warehouseID == 7){
                    //nearest warehouse is frankfurt
                    customerWarehouseID = 6;
                }
                //warehouse in mumbai
                else if(warehouseID == 8){
                    //nearest warehouse is singapore
                    customerWarehouseID = 9;
                }
                //warehouse in singapore
                else if(warehouseID == 9){
                    //nearest warehouse is mumbai
                    customerWarehouseID = 8;
                }
                //warehouse in tokyo
                else if(warehouseID == 10){
                    //nearest warehouse is sydney
                    customerWarehouseID = 11;
                }
                //warehouse in sydney
                else if(warehouseID == 11){
                    //nearest warehouse is tokyo
                    customerWarehouseID = 10;
                }

                // jump one warehouse (low probability)
                if(randomNumber(1, 100, gen) > args.getLocality()){
                    jump1++;
                    //warehouse in california
                    if(warehouseID == 0){
                        //nearest warehouse jump is virginia
                        customerWarehouseID = 2;
                    }
                    //warehouse in canada
                    else if(warehouseID == 1){
                        //nearest warehouse jump is sao paulo
                        customerWarehouseID = 3;
                    }
                    //warehouse in virginia
                    else if(warehouseID == 2){
                        //nearest warehouse jump is california
                        customerWarehouseID = 0;
                    }
                    //warehouse in sao paulo
                    else if(warehouseID == 3){
                        //nearest warehouse jump is canada
                        customerWarehouseID = 1;
                    }
                    //warehouse in ireland
                    else if(warehouseID == 4){
                        //nearest warehouse jump is frankfurt
                        customerWarehouseID = 6;
                    }
                    //warehouse in paris
                    else if(warehouseID == 5){
                        //nearest warehouse jump is stockholm
                        customerWarehouseID = 7;
                    }
                    //warehouse in frankfurt
                    else if(warehouseID == 6){
                        //nearest warehouse jump is ireland
                        customerWarehouseID = 4;
                    }
                    //warehouse in stockholm
                    else if(warehouseID == 7){
                        //nearest warehouse jump is paris
                        customerWarehouseID = 5;
                    }
                    //warehouse in mumbai
                    else if(warehouseID == 8){
                        //nearest warehouse jump is tokyo
                        customerWarehouseID = 10;
                    }
                    //warehouse in singapore
                    else if(warehouseID == 9){
                        //nearest warehouse jump is sydney
                        customerWarehouseID = 11;
                    }
                    //warehouse in tokyo
                    else if(warehouseID == 10){
                        //nearest warehouse jump is mumbai
                        customerWarehouseID = 8;
                    }
                    //warehouse in sydney
                    else if(warehouseID == 11){
                        //nearest warehouse jump is singapore
                        customerWarehouseID = 9;
                    }

                    // jump 2 warehouse (very low probability)
                    if(randomNumber(1, 100, gen) > args.getLocality()){
                        jump1--; jump2++;
                        //warehouse in california
                        if(warehouseID == 0){
                            //nearest warehouse jump2 is sao paulo
                            customerWarehouseID = 3;
                        }
                        //warehouse in canada
                        else if(warehouseID == 1){
                            //nearest warehouse jump2 is ireland
                            customerWarehouseID = 4;
                        }
                        //warehouse in virginia
                        else if(warehouseID == 2){
                            //nearest warehouse jump2 is paris
                            customerWarehouseID = 5;
                        }
                        //warehouse in sao paulo
                        else if(warehouseID == 3){
                            //nearest warehouse jump2 is california
                            customerWarehouseID = 0;
                        }
                        //warehouse in ireland
                        else if(warehouseID == 4){
                            //nearest warehouse jump2 is stockholm
                            customerWarehouseID = 7;
                        }
                        //warehouse in paris
                        else if(warehouseID == 5){
                            //nearest warehouse jump2 is virginia
                            customerWarehouseID = 2;
                        }
                        //warehouse in frankfurt
                        else if(warehouseID == 6){
                            //nearest warehouse jump2 is singapore
                            customerWarehouseID = 9;
                        }
                        //warehouse in stockholm
                        else if(warehouseID == 7){
                            //nearest warehouse jump is ireland
                            customerWarehouseID = 4;
                        }
                        //warehouse in mumbai
                        else if(warehouseID == 8){
                            //nearest warehouse jump2 is sydney
                            customerWarehouseID = 11;
                        }
                        //warehouse in singapore
                        else if(warehouseID == 9){
                            //nearest warehouse jump2 is frankfurt
                            customerWarehouseID = 6;
                        }
                        //warehouse in tokyo
                        else if(warehouseID == 10){
                            //nearest warehouse jump2 is stockholm
                            customerWarehouseID = 7;
                        }
                        //warehouse in sydney
                        else if(warehouseID == 11){
                            //nearest warehouse jump2 is mumbai
                            customerWarehouseID = 8;
                        }

                        // jump 3 warehouse (very low probability)
                        if(randomNumber(1, 100, gen) > args.getLocality()){
                            jump2--; jump3++;
                            if(warehouseID == 0){
                                customerWarehouseID = 4;
                            }
                            else if(warehouseID == 1){
                                customerWarehouseID = 5;
                            }
                            else if(warehouseID == 2){
                                customerWarehouseID = 6;
                            }
                            else if(warehouseID == 3){
                                customerWarehouseID = 7;
                            }
                            else if(warehouseID == 4){
                                customerWarehouseID = 0;
                            }
                            else if(warehouseID == 5){
                                customerWarehouseID = 1;
                            }
                            else if(warehouseID == 6){
                                customerWarehouseID = 2;
                            }
                            else if(warehouseID == 7){
                                customerWarehouseID = 3;
                            }
                            else if(warehouseID == 8){
                                customerWarehouseID = 4;
                            }
                            else if(warehouseID == 9){
                                customerWarehouseID = 5;
                            }
                            else if(warehouseID == 10){
                                customerWarehouseID = 6;
                            }
                            else if(warehouseID == 11){
                                customerWarehouseID = 7;
                            }

                            // jump 4 warehouse (very low probability)
                            if(randomNumber(1, 100, gen) > args.getLocality()){
                                jump3--; jump4++;
                                if(warehouseID == 0){
                                    customerWarehouseID = 5;
                                }
                                else if(warehouseID == 1){
                                    customerWarehouseID = 6;
                                }
                                else if(warehouseID == 2){
                                    customerWarehouseID = 7;
                                }
                                else if(warehouseID == 3){
                                    customerWarehouseID = 8;
                                }
                                else if(warehouseID == 4){
                                    customerWarehouseID = 9;
                                }
                                else if(warehouseID == 5){
                                    customerWarehouseID = 0;
                                }
                                else if(warehouseID == 6){
                                    customerWarehouseID = 1;
                                }
                                else if(warehouseID == 7){
                                    customerWarehouseID = 2;
                                }
                                else if(warehouseID == 8){
                                    customerWarehouseID = 3;
                                }
                                else if(warehouseID == 9){
                                    customerWarehouseID = 4;
                                }
                                else if(warehouseID == 10){
                                    customerWarehouseID = 5;
                                }
                                else if(warehouseID == 11){
                                    customerWarehouseID = 6;
                                }
                            }
                        }
                    }
                }
            }
        }
        else {

            if (randomNumber(1, 100, gen) <= args.getLocality()) {
                customerWarehouseID = warehouseID;
            }
            else {
                int inc = 1; int dec = 1;
                boolean incrementing = true;
                do {
                    if(incrementing){
                        if(warehouseID+inc < warehouseCount){
                            customerWarehouseID = warehouseID+inc;
                            inc++;
                        }
                        else {
                            incrementing = false;
                        }
                    }
                    else {
                        if(warehouseID-dec >= 0){
                            customerWarehouseID = warehouseID-dec;
                            dec++;
                        }
                        else {
                            break;
                        }
                    }
                }
                while(randomNumber(1, 100, gen) > args.getLocality());
            }
        }
        
        List<Integer> dest = new ArrayList<>();
        dest.add(warehouseID);
        if (!dest.contains(customerWarehouseID)) {
            dest.add(customerWarehouseID);
        }
        short [] tempdst = new short[dest.size()];
        short i = 0;
        for(int u : dest.stream().sorted().collect(Collectors.toList())){
            tempdst[i] = (short)u;
            i++;
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

        return tempdst;
    }

    public short[] doOrderStatus() {
        dest1++;
        numOrderStatusTx++;
        partitionsAccessedAllTxs += 1;
        return new short[]{(short)warehouseID};
    }

    public short[] doDelivery() {
        dest1++;
        numDeliveryTx++;
        partitionsAccessedAllTxs += 1;
        return new short[]{(short)warehouseID};
    }

    public short[] doStockLevel() {
        dest1++;
        numStockLevelTx++;
        partitionsAccessedAllTxs += 1;
        return new short[]{(short)warehouseID};
    }

    public static int randomNumber(int min, int max, Random r) {
        return (int) (r.nextDouble() * (max - min + 1) + min);
    }
}
