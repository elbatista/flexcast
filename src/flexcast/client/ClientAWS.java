package flexcast.client;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import base.Host;
import base.Node;
import flexcast.messages.Message.TransactionType;
import flexcast.messages.Message.Type;
import flexcast.reconfig.View;
import proxies.ClientProxy;
import util.ArgsParser;
import util.FileManager;
import util.OrderItem;
import util.Stats;
import flexcast.messages.Message;

public class ClientAWS extends ClientProxy {
    protected ArgsParser args;
    protected int seqNumber, totalTime;
    protected CyclicBarrier syncAllConnections;
    protected FileManager files;
    private int localityPercentage = 0;
    private int [] destsSizes;
    protected int [][] wloadDist2dests;
    protected int [][][] wloadDist3dests;
    protected final Random gen;
    protected final Random thinkTimeRand;
    private short warehouse;
    private boolean sendPayload, tt, includeLocalMsgs;
    private int gc=0, dagTop = 1;
    private HashMap<Short, String> nearestWHs = new HashMap<>();
    double AcumTt = 0;
    int TtCount = 0;

    // Tpcc workload distribution
    private static final int newOrderWeight = 45;
    private static final int paymentWeight = 43;
    private static final int orderStatusWeight = 4;
    private static final int deliveryWeight = 4;
    private static final int stockLevelWeight = 4;

    public ClientAWS(short id, ArgsParser args, boolean start){
        super(id);
        this.args = args;
        totalTime = args.getDuration();
        this.files = new FileManager();
        this.sendPayload = args.shouldSendPayload();
        this.tt = args.thinkTime();
        this.includeLocalMsgs = args.includeLocalMsgs();
        this.localityPercentage = args.getLocality();
        this.warehouse = (short) args.getHomeWarehouse();
        this.gc = args.getGC();
        dagTop = args.getDAGTop();
        if(!args.getLog()) setPrint(false);
        this.gen = new Random(System.nanoTime());
        thinkTimeRand = new Random(System.nanoTime());
        ArrayList<Node> nodes = files.loadHosts();
        FileManager.loadLocalityFile(nearestWHs);
        syncAllConnections = new CyclicBarrier(nodes.size()+1);
        currentView = new View(0, nodes);
        connectToServers(syncAllConnections);
        numNodes = (short) nodes.size();
        destsSizes = new int [numNodes];
        wloadDist2dests = new int [numNodes][numNodes];
        wloadDist3dests = new int [numNodes][numNodes][numNodes];
        if(start) start();
    }
    
    // generates an unique message id, based on the client id
    private int nextSeqNumber(){
        seqNumber++;
        while((seqNumber % args.getClientCount()) != getId())
            seqNumber++;
        return seqNumber;
    }

    private void start() {
        if(gc > -1)
            printF("Started AWS FlexCast GC Client", "["+getId()+"]");
        else
            printF("Start FlexCast ClientAWS", "["+getId()+"]");

        // wait all netty threads connect to all servers
        try {syncAllConnections.await();} catch(InterruptedException|BrokenBarrierException e){printF("Broken barrier!!!!");}

        printF("Connected to all servers!");
        printF("DAG TOPOLY:", dagTop);
        printF("Initial view:", currentView);
        // sleep(10000);

        // send initialization message to all servers
        sendInitMessage();
        // send ready message to a server
        // the server will reply when all clients are ready, then we "guarantee" all clients start at (~) the same time
        sendReadyMessage();
        printF("All other clients ready!");

        if(gc > -1){
            runGCClient();
        }
        else {
            printF("Started AWS FlexCast experiment");
            if(args.getNumPartitions() > 0) print (args.getNumPartitions(), "partitions");
            printF("Locality:", localityPercentage, "%");
            printF("My home warehouse:", warehouse);
            if(args.getNumMessages() > 0) printF("Will send", args.getNumMessages(), "messages");
            if(sendPayload) printF("Sending a TPCC-like PAYLOAD in messages");
            if(tt) printF("Using Think Time");
            if(includeLocalMsgs) printF("Including LOCAL messages");
            else printF("ONLY GLOBAL messages");
            stats = new Stats(totalTime, numNodes);

            long startTime = System.nanoTime();
            long now;
            long elapsed = 0;//, usLat = startTime;
            int totalMsgs=0;

            while ((elapsed / 1e9) < totalTime) {
                
                Message m = newMessage();
                generatePayload(m);

                now = System.nanoTime();
                multicast(m);
                stats.store((System.nanoTime() - now) / 1000, (m.getDst().length > 1));

                elapsed = (now - startTime);
                
                destsSizes[m.getDst().length-1]++;

                computeDistribution(m);

                if(tt) thinkTime();
                
                //usLat = now;
                totalMsgs++;
                if(args.getNumMessages() > 0 && totalMsgs == args.getNumMessages()) break;
            }

            if (stats.getCount() > 0) {
                try {Files.createDirectories(Paths.get("results" + (args.getRegion().equals("") ? "" : "/"+args.getRegion())));} catch (IOException e) {}
                stats.persist("results/" + getId() + "-stats-client.txt", 15);
                stats.persistPerNodes("results/" + getId() + "-stats-client-per-node.txt", 15);
                
                if(!args.getRegion().equals("")){
                    stats.persist("results/"+args.getRegion() + "/" + getId() + "-stats-client.txt", 15);
                    stats.persistPerNodes("results/"+args.getRegion() + "/" + getId() + "-stats-client-per-node.txt", 15);
                }

                printF("LOCAL STATS:", stats);
            }
            
            sendEndMessage();            
            for(int i = 0; i < destsSizes.length; i++) printF("# of msgs to", i+1, "dests:", destsSizes[i]);
            printWloadDistribution();
            printF("Finished AWS FlexCast experiment. Elapsed: ", elapsed / 1e9, "seconds");
        }
        exit();
    }

    private void generatePayload(Message m) {
        if(!sendPayload) {
            m.setTransaction(Message.TransactionType.NOPAYLOAD);
            return;
        }
        int transactionType = randomNumber(1, 100, gen);
        m.setOrderDate(new Date());
        if (transactionType <= newOrderWeight) {
            m.setTransaction(Message.TransactionType.NEW);
            int numItems = randomNumber(5, 15, gen);
            for (int i = 0; i < numItems; i++) {
                m.getItems().add(new OrderItem(randomNumber(1, 100000, gen), randomNumber(1, 10, gen)));
            }
        } else if (transactionType <= newOrderWeight + paymentWeight) {
            m.setTransaction(Message.TransactionType.PAYMENT);
            m.setPaymentAmount(randomNumber(1, 5000, gen));
        } else if (transactionType <= newOrderWeight + paymentWeight + orderStatusWeight) {
            m.setTransaction(Message.TransactionType.STATUS);
        } else if (transactionType <= newOrderWeight + paymentWeight + orderStatusWeight + deliveryWeight) {
            m.setTransaction(Message.TransactionType.DELIVERY);
            m.setCarrierid_or_threshold(randomNumber(1, 10, gen));
        } else if (transactionType <= newOrderWeight + paymentWeight + orderStatusWeight + deliveryWeight + stockLevelWeight) {
            m.setTransaction(Message.TransactionType.STOCK);
            m.setCarrierid_or_threshold(randomNumber(10, 20, gen));
        }
    }

    private void thinkTime() {
        /*
         * Tt = -log(r) * u 
         * where: log  = natural log (base e)  
         * Tt  = think time  
         * r  = random number uniformly distributed between 0 and 1  
         * u  = mean think time 
         * 
         * Each distribution may be truncated at 10 times its mean value
         */
        double r = thinkTimeRand.nextDouble();
        double u = 100;
        double Tt = -Math.log(r) * u;
        if(Tt > (1000)) Tt = 1000;
        sleep((long)Tt);
    }

    private void runGCClient() {
        long startTime = System.nanoTime(), now;
        long elapsed = 0;

        printF("GC Interval:", gc, "(ms)");

        while ((elapsed / 1e9) < (totalTime+2)) {
            // envia msg de "flush"
            Message m = newMessageTo(allDests());
            m.setTransaction(TransactionType.NOPAYLOAD);
            multicast(m);
            printF("Sent and received all replies for flush message", m);
            // apos receber resposta de todos nodes (todos entregaram a msg de flush)
            // envia msg de GC referente a msg do flush
            sendGCMessage(m.getId());
            printF("Sent and received all replies GC for msg", m.getId());
            if(gc > 0 ) sleep(gc);
            now = System.nanoTime();
            elapsed = (now - startTime);
        }

        sendEndMessage();            
        printF("Finished AWS FlexCast GC Client. Elapsed: ", elapsed / 1e9, "seconds");
    }

    protected void printWloadDistribution() {
        printF("Wload for destination size 2:");
        for(int i = 0; i < numNodes; i++)
            for(int j = 0; j < numNodes; j++)
                if(wloadDist2dests[i][j] > 0) printF("# of msgs to [",i, j, "]:", wloadDist2dests[i][j]);

        printF("Wload for destination size 3:");
        for(int i = 0; i < numNodes; i++)
            for(int j = 0; j < numNodes; j++)
                for(int k = 0; k < numNodes; k++)
                    if(wloadDist3dests[i][j][k] > 0) printF("# of msgs to [",i, j, k, "]:", wloadDist3dests[i][j][k]);
    }

    protected void computeDistribution(Message m) {
        if (m.getDst().length == 2){
            wloadDist2dests[m.getDst()[0]][m.getDst()[1]]++;
        } else if (m.getDst().length == 3){
            wloadDist3dests[m.getDst()[0]][m.getDst()[1]][m.getDst()[2]]++;
        }
    }

    protected Message newMessageTo(short... dst){
        Message m = new Message(nextSeqNumber(), currentView.getId());
        m.setType(Type.MSG);
        m.setCliId(getId());
        m.setDst(dst);
        return m;
    }

    private Message newMessage(){
        Message m = new Message(nextSeqNumber(), currentView.getId());
        m.setType(Type.MSG);
        m.setDst(generateDests());
        m.setCliId(getId());
        return m;
    }

    private short[] generateDests(){

        if(includeLocalMsgs && randomNumber(1, 100, gen) <= 90){
            return new short[]{warehouse};
        }

        if(localityPercentage == 0){
            return generateRandDests();
        }
        if(randomNumber(1, 100, gen) <= localityPercentage) 
            return generate2Dests();
        return generate3Dests();
    }

    private short[] allDests() {
        short [] tempdst = new short[currentView.getNodes().size()];
        int i = 0;
        for(Node n : currentView.getNodes()){
            tempdst[i] = n.getId();
            i++;
        }
        return tempdst;
    }

    private short[] generateRandDests() {
        Set<Short> uniqueNumbers = new HashSet<>();
        int size = randomNumber(2, numNodes, gen); // only global
        while (uniqueNumbers.size() < size)
            uniqueNumbers.add((short)randomNumber(0, numNodes-1, gen));
        short [] tempdst = new short[size];
        short i = 0;
        for(short u : uniqueNumbers){
            tempdst[i] = u;
            i++;
        }
        return currentView.sortByCDAGPosition(tempdst);
    }

    private short[] generate2Dests(){
        short [] tempdst = new short[2];
        tempdst[0] = warehouse;

        if(randomNumber(1, 100, gen) <= localityPercentage)
            tempdst[1] = getNearestWH(0);
        else 
            tempdst[1] = getNearestWH(1);

        // Arrays.sort(tempdst);

        return currentView.sortByCDAGPosition(tempdst);
    }

    private short[] generate3Dests(){
        short [] tempdst = new short[3];
        tempdst[0] = warehouse;
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
        // Arrays.sort(finaldst);

        return currentView.sortByCDAGPosition(finaldst);
    }

    private short getNearestWH(int index) {
        short tempdst = -1;

        try{tempdst = Short.valueOf(nearestWHs.get((short)warehouse).split(" ")[index].trim());} catch(Exception e){}

        if(tempdst == -1){
            // simply get the next HW in order of id
            tempdst = (short)(warehouse+1);
            if(tempdst == numNodes) tempdst = (short)(warehouse-1);
        }
        return tempdst;
    }

    protected void changeView(Message m) {

        if(m.getViewId() == currentView.getId()){
            printF("I am already in the new view, ignoring.");
            return;
        }
        
        // get the nodes from the current view
        // change their position acording to new overlay
        int pos = 0;
        ArrayList<Node> newnodes = new ArrayList<>();
        for(short s : m.getNewOverlay()){
            Host h = currentView.getHostFromNode(s);
            newnodes.add(new Node(s,h,pos));
            pos++;
        }
        View nextView = new View(m.getViewId(), newnodes);        
        // transfer fisical connections from prev view
        nextView.setConnections(currentView.getConnections());
        // set current view as next, and next to null
        currentView = nextView;
        numNodes = (short)currentView.getNodes().size();
        // nextView = null;
        // process any buffered message in the new view
        printF("Changed to a new view:", currentView);
    }

    public static int randomNumber(int min, int max, Random r) {
        return (int) (r.nextDouble() * (max - min + 1) + min);
    }

}