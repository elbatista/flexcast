package flexcast.client;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.stream.Collectors;
import base.Node;
import flexcast.messages.Message.TransactionType;
import flexcast.messages.Message.Type;
import proxies.ClientProxy;
import util.ArgsParser;
import util.FileManager;
import util.OrderItem;
import util.Stats;
import flexcast.messages.Message;

public class GapsClient extends ClientProxy {
    protected ArgsParser args;
    protected int seqNumber, totalTime, dagTop = 1;
    protected CyclicBarrier syncAllConnections;
    protected FileManager files;
    protected final Random gen;
    private short warehouse;
    private HashMap<Short, String> nearestWHs = new HashMap<>();

    private int gap1, gap2, gap4, gap8;

    public GapsClient(short id, ArgsParser args){
        super(id);
        this.args = args;
        this.totalTime = args.getDuration();
        this.files = new FileManager();
        this.warehouse = (short) args.getHomeWarehouse();
        this.dagTop = args.getDAGTop();
        this.gen = new Random(System.nanoTime());
        if(!args.getLog()) setPrint(false);
        ArrayList<Node> nodes = files.loadHosts();
        syncAllConnections = new CyclicBarrier(nodes.size()+1);
        for(Node server : nodes) connectTo(server, syncAllConnections);
        numNodes = (short) nodes.size();
        FileManager.loadLocalityFile(nearestWHs);
        printF(nearestWHs);
        start();
    }
    
    // generates an unique message id, based on the client id
    private int nextSeqNumber(){
        seqNumber++;
        while((seqNumber % args.getClientCount()) != getId())
            seqNumber++;
        return seqNumber;
    }

    private void start() {
        printF("Start FlexCast GapsClient");

        // wait all netty threads connect to all servers
        try {syncAllConnections.await();} catch(InterruptedException|BrokenBarrierException e){printF("Broken barrier!!!!");}

        printF("Connected to all servers!");
        printF("DAG TOPOLY:", dagTop);
        sleep(1000);

        // send initialization message to all servers
        sendInitMessage();
        // send ready message to a server
        // the server will reply when all clients are ready, then we "guarantee" all clients start at (~) the same time
        sendReadyMessage();
        printF("All other clients ready!");
        printF("Started AWS FlexCast GapsClient experiment");
        printF("My home warehouse:", warehouse);
        if(args.getNumMessages() > 0) printF("Will send", args.getNumMessages(), "messages");
        stats = new Stats(totalTime, numNodes);

        long startTime = System.nanoTime();
        long now;
        long elapsed = 0;//, usLat = startTime;
        int totalMsgs=0;

        while ((elapsed / 1e9) < totalTime) {
            
            Message m = newMessage();

            if(m.getDst() == null) continue;

            now = System.nanoTime();

            m.setTransaction(TransactionType.NOPAYLOAD);

            multicast(m);

            stats.store((System.nanoTime() - now) / 1000, (m.getDst().length > 1));
            
            elapsed = (now - startTime);
            
            totalMsgs++;
            if(args.getNumMessages() > 0 && totalMsgs == args.getNumMessages()) break;
        }

        if (stats.getCount() > 0) {
            try {Files.createDirectories(Paths.get("results" + (args.getRegion().equals("") ? "" : "/"+args.getRegion())));} catch (IOException e) {}
            stats.persist("results" + (args.getRegion().equals("") ? "" : "/"+args.getRegion()) + "/" + getId() + "-stats-gapsclient.txt", 15);
            stats.persistPerNodes("results" + (args.getRegion().equals("") ? "" : "/"+args.getRegion()) + "/" + getId() + "-stats-gapsclient-per-node.txt", 15);
            printF("LOCAL STATS:", stats);
        }
        
        sendEndMessage();            
        printF("Finished AWS FlexCast GapsClient experiment. Elapsed: ", elapsed / 1e9, "seconds");
        printF("gaps 1 2 4 8:", gap1, gap2, gap4, gap8);
        exit();
    }

    protected Message newMessageTo(short... dst){
        Message m = new Message(nextSeqNumber());
        m.setType(Type.MSG);
        m.setCliId(getId());
        m.setDst(dst);
        return m;
    }

    private Message newMessage(){
        Message m = new Message(nextSeqNumber());
        m.setType(Type.MSG);
        m.setDst(generateDests());
        m.setCliId(getId());
        return m;
    }

    private short[] generateDests(){

        int rand = randomNumber(1, 100, gen);

        short [] tempdst = new short[2];
        tempdst[0] = warehouse;

        if(rand <= 25){
            // gap 1 (see config/locality.conf)
            tempdst[1] = getNearestWH(3);
            if(tempdst[1] == -1) return null;
            gap1++;
        } else if(rand <= 50){
            // gap 2
            tempdst[1] = getNearestWH(4);
            if(tempdst[1] == -1) return null;
            gap2++;
        } else if(rand <= 75){
            // gap 4
            tempdst[1] = getNearestWH(5);
            if(tempdst[1] == -1) return null;
            gap4++;
        } else {
            // gap 8
            tempdst[1] = getNearestWH(6);
            if(tempdst[1] == -1) return null;
            // printF("Gap 8:", Arrays.toString(tempdst));
            gap8++;
        }

        Arrays.sort(tempdst);

        return tempdst;
        
    }

    private short[] generateRandDests() {
        Set<Short> uniqueNumbers = new HashSet<>();
        int size = randomNumber(2, numNodes, gen); // only global
        while (uniqueNumbers.size() < size)
            uniqueNumbers.add((short)randomNumber(0, numNodes-1, gen));
        short [] tempdst = new short[size];
        short i = 0;
        for(short u : uniqueNumbers.stream().sorted().collect(Collectors.toList())){
            tempdst[i] = u;
            i++;
        }
        return tempdst;
    }

    private short getNearestWH(int index) {
        short tempdst = -1;

        try{tempdst = Short.valueOf(nearestWHs.get((short)warehouse).split(" ")[index].trim());} catch(Exception e){}

        // if(tempdst == -1){
        //     // simply get the next HW in order of id
        //     tempdst = (short)(warehouse+1);
        //     if(tempdst == numNodes) tempdst = (short)(warehouse-1);
        // }
        return tempdst;
    }

    public static int randomNumber(int min, int max, Random r) {
        return (int) (r.nextDouble() * (max - min + 1) + min);
    }

}