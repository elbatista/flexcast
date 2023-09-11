package flexcast.client;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.stream.Collectors;
import base.Node;
import flexcast.messages.Message.Type;
import proxies.ClientProxy;
import util.ArgsParser;
import util.FileManager;
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
    private short warehouse;
    private int gc=0, dagTop = 1;
    
    public ClientAWS(short id, ArgsParser args, boolean start){
        super(id);
        this.args = args;
        totalTime = args.getDuration();
        this.files = new FileManager();
        this.localityPercentage = args.getLocality();
        this.warehouse = (short) args.getHomeWarehouse();
        this.gc = args.getGC();
        dagTop = args.getDAGTop();
        if(!args.getLog()) setPrint(false);
        this.gen = new Random(System.nanoTime());
        ArrayList<Node> nodes = files.loadHosts();
        syncAllConnections = new CyclicBarrier(nodes.size()+1);
        for(Node server : nodes) connectTo(server, syncAllConnections);
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
            printF("Started AWS FlexCast GC Client");
        else
            printF("Start FlexCast ClientAWS");

        // wait all netty threads connect to all servers
        try {syncAllConnections.await();} catch(InterruptedException|BrokenBarrierException e){printF("Broken barrier!!!!");}

        printF("Connected to all servers!");
        printF("DAG TOPOLY:", dagTop);
        //sleep(1000);

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
            stats = new Stats(totalTime, numNodes);

            long startTime = System.nanoTime(), now;
            long elapsed = 0, usLat = startTime;
            int totalMsgs=0;

            while ((elapsed / 1e9) < totalTime) {
                
                Message m = newMessage();

                multicast(m);
                now = System.nanoTime();
                stats.store((now - usLat) / 1000, (m.getDst().length > 1));
                elapsed = (now - startTime);
                
                destsSizes[m.getDst().length-1]++;

                computeDistribution(m);
                
                usLat = now;
                totalMsgs++;
                if(args.getNumMessages() > 0 && totalMsgs == args.getNumMessages()) break;
            }

            if (stats.getCount() > 0) {
                try {Files.createDirectories(Paths.get("results" + (args.getRegion().equals("") ? "" : "/"+args.getRegion())));} catch (IOException e) {}
                stats.persist("results" + (args.getRegion().equals("") ? "" : "/"+args.getRegion()) + "/" + getId() + "-stats-client.txt", 15);
                stats.persistPerNodes("results" + (args.getRegion().equals("") ? "" : "/"+args.getRegion()) + "/" + getId() + "-stats-client-per-node.txt", 15);
                printF("LOCAL STATS:", stats);
            }
            
            sendEndMessage();            
            for(int i = 0; i < destsSizes.length; i++) printF("# of msgs to", i+1, "dests:", destsSizes[i]);
            printWloadDistribution();
            printF("Finished AWS FlexCast experiment. Elapsed: ", elapsed / 1e9, "seconds");
        }
        exit();
    }

    private void runGCClient() {
        long startTime = System.nanoTime(), now;
        long elapsed = 0;

        printF("GC Interval:", gc, "(ms)");

        while ((elapsed / 1e9) < (totalTime+2)) {
            // envia msg de "flush"
            Message m = newMessageTo(allDests());
            multicast(m);
            printF("Sent and received all replies for flush message", m);
            // apos receber resposta de todos nodes (todos entregaram a msg de flush)
            // envia msg de GC referente a msg do flush
            sendGCMessage(m.getId());
            printF("Sent and received all replies GC for msg", m.getId());
            if(gc > 0 )sleep(gc);
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

        if(randomNumber(1, 100, gen) < 99){
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
        short [] tempdst = new short[numNodes];
        for(short s = 0; s < numNodes; s++)
            tempdst[s] = s;
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

    private short[] generate2Dests(){
        short [] tempdst = new short[2];
        tempdst[0] = warehouse;

        if(randomNumber(1, 100, gen) <= localityPercentage)
            tempdst[1] = getNearestWH(warehouse);
        else 
            tempdst[1] = getSecondNearestWH(warehouse);

        Arrays.sort(tempdst);

        return tempdst;
    }

    private short[] generate3Dests(){
        short [] tempdst = new short[3];
        tempdst[0] = warehouse;
        if(randomNumber(1, 100, gen) <= localityPercentage){
            tempdst[1] = getNearestWH(warehouse);
            tempdst[2] = getSecondNearestWH(warehouse);
        }else {
            tempdst[1] = getSecondNearestWH(warehouse);
            tempdst[2] = getThirdNearestWH(warehouse);
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

    private short getNearestWH(short warehouseparam) {
        if(numNodes == 9){
            if(dagTop == 1){
                switch(warehouseparam){
                    case 0: return 1;
                    case 1: return 2;
                    case 2: return 1;
                    case 3: return 4;
                    case 4: return 5;
                    case 5: return 4;
                    case 6: return 7;
                    case 7: return 8;
                    case 8: return 7;
                    default: return warehouseparam;
                }
            } else if(dagTop == 3){
                switch(warehouseparam){
                    case 0: return 1;
                    case 1: return 2;
                    case 2: return 1;
                    case 3: return 4;
                    case 4: return 5;
                    case 5: return 4;
                    case 6: return 7;
                    case 7: return 8;
                    case 8: return 7;
                    default: return warehouseparam;
                }
            }
        }else if(numNodes == 12){
            if(dagTop == 1){
                switch(warehouseparam){
                    case 0: return 1;
                    case 1: return 2;
                    case 2: return 1;
                    case 3: return 2;
                    case 4: return 5;
                    case 5: return 6;
                    case 6: return 7;
                    case 7: return 6;
                    case 8: return 9;
                    case 9: return 10;
                    case 10: return 9;
                    case 11: return 10;
                    default: return warehouseparam;
                }
            } else if(dagTop == 2){
                switch(warehouseparam){
                    case 0: return 1;
                    case 1: return 2;
                    case 2: return 3;
                    case 3: return 2;
                    case 4: return 5;
                    case 5: return 6;
                    case 6: return 7;
                    case 7: return 6;
                    case 8: return 9;
                    case 9: return 10;
                    case 10: return 9;
                    case 11: return 10;
                    default: return warehouseparam;
                }
            } else if(dagTop == 3){
                switch(warehouseparam){
                    case 0: return 1;
                    case 1: return 2;
                    case 2: return 3;
                    case 3: return 2;
                    case 4: return 5;
                    case 5: return 6;
                    case 6: return 5;
                    case 7: return 6;
                    case 8: return 9;
                    case 9: return 10;
                    case 10: return 9;
                    case 11: return 10;
                    default: return warehouseparam;
                }
            }
        } 
        else if(numNodes == 13){
            switch(warehouseparam){
                case 0: return 1;
                case 1: return 2;
                case 2: return 3;
                case 3: return 4;
                case 4: return 5;
                case 5: return 6;
                case 6: return 7;
                case 7: return 8;
                case 8: return 9;
                case 9: return 10;
                case 10: return 11;
                case 11: return 12;
                case 12: return 11;
                default: return warehouseparam;
            }
        }
        return warehouseparam;
    }

    private short getSecondNearestWH(short warehouseparam) {
        if(numNodes == 9){
            if(dagTop == 1){
                switch(warehouseparam){
                    case 0: return 2;
                    case 1: return 3;
                    case 2: return 0;
                    case 3: return 5;
                    case 4: return 2;
                    case 5: return 3;
                    case 6: return 8;
                    case 7: return 5;
                    case 8: return 6;
                    default: return warehouseparam;
                }
            } else if(dagTop == 3){
                switch(warehouseparam){
                    case 0: return 2;
                    case 1: return 3;
                    case 2: return 0;
                    case 3: return 5;
                    case 4: return 2;
                    case 5: return 3;
                    case 6: return 8;
                    case 7: return 6;
                    case 8: return 6;
                    default: return warehouseparam;
                }
            }
        }if(numNodes == 12){
            if(dagTop == 1){
                switch(warehouseparam){
                    case 0: return 2;
                    case 1: return 3;
                    case 2: return 0;
                    case 3: return 1;
                    case 4: return 6;
                    case 5: return 7;
                    case 6: return 4;
                    case 7: return 5;
                    case 8: return 10;
                    case 9: return 11;
                    case 10: return 8;
                    case 11: return 9;
                    default: return warehouseparam;
                }
            } else if(dagTop == 2){
                switch(warehouseparam){
                    case 0: return 2;
                    case 1: return 3;
                    case 2: return 0;
                    case 3: return 1;
                    case 4: return 6;
                    case 5: return 7;
                    case 6: return 4;
                    case 7: return 5;
                    case 8: return 10;
                    case 9: return 11;
                    case 10: return 8;
                    case 11: return 9;
                    default: return warehouseparam;
                }
            } else if(dagTop == 3){
                switch(warehouseparam){
                    case 0: return 2;
                    case 1: return 3;
                    case 2: return 0;
                    case 3: return 1;
                    case 4: return 6;
                    case 5: return 7;
                    case 6: return 4;
                    case 7: return 5;
                    case 8: return 10;
                    case 9: return 11;
                    case 10: return 8;
                    case 11: return 9;
                    default: return warehouseparam;
                }
            }
        }
        else if(numNodes == 13){
            switch(warehouseparam){
                case 0: return 2;
                case 1: return 3;
                case 2: return 0;
                case 3: return 1;
                case 4: return 2;
                case 5: return 7;
                case 6: return 8;
                case 7: return 5;
                case 8: return 6;
                case 9: return 11;
                case 10: return 12;
                case 11: return 9;
                case 12: return 10;
                default: return warehouseparam;
            }
        }
        return warehouseparam;
    }

    private short getThirdNearestWH(short warehouseparam) {
        if(numNodes == 9){
            if(dagTop == 1){
                switch(warehouseparam){
                    case 0: return 3;
                    case 1: return 4;
                    case 2: return 5;
                    case 3: return 6;
                    case 4: return 1;
                    case 5: return 2;
                    case 6: return 3;
                    case 7: return 4;
                    case 8: return 5;
                    default: return warehouseparam;
                }
            }else if(dagTop == 3){
                switch(warehouseparam){
                    case 0: return 3;
                    case 1: return 4;
                    case 2: return 5;
                    case 3: return 0;
                    case 4: return 1;
                    case 5: return 2;
                    case 6: return 8;
                    case 7: return 5;
                    case 8: return 6;
                    default: return warehouseparam;
                }
            }
        }if(numNodes == 12){
            if(dagTop == 1){
                switch(warehouseparam){
                    case 0: return 3;
                    case 1: return 4;
                    case 2: return 5;
                    case 3: return 0;
                    case 4: return 7;
                    case 5: return 2;
                    case 6: return 3;
                    case 7: return 4;
                    case 8: return 5;
                    case 9: return 6;
                    case 10: return 7;
                    case 11: return 8;
                    default: return warehouseparam;
                }
            } else if(dagTop == 2){
                switch(warehouseparam){
                    case 0: return 3;
                    case 1: return 4;
                    case 2: return 5;
                    case 3: return 6;
                    case 4: return 7;
                    case 5: return 8;
                    case 6: return 3;
                    case 7: return 4;
                    case 8: return 5;
                    case 9: return 6;
                    case 10: return 7;
                    case 11: return 8;
                    default: return warehouseparam;
                }
            } else if(dagTop == 3){
                switch(warehouseparam){
                    case 0: return 3;
                    case 1: return 4;
                    case 2: return 5;
                    case 3: return 0;
                    case 4: return 1;
                    case 5: return 2;
                    case 6: return 3;
                    case 7: return 4;
                    case 8: return 11;
                    case 9: return 11;
                    case 10: return 7;
                    case 11: return 8;
                    default: return warehouseparam;
                }
            }
        }
        else if(numNodes == 13){
            switch(warehouseparam){
                case 0: return 3;
                case 1: return 4;
                case 2: return 5;
                case 3: return 0;
                case 4: return 2;
                case 5: return 8;
                case 6: return 9;
                case 7: return 10;
                case 8: return 5;
                case 9: return 12;
                case 10: return 7;
                case 11: return 8;
                case 12: return 9;
                default: return warehouseparam;
            }
        }
        return warehouseparam;
    }

    public static int randomNumber(int min, int max, Random r) {
        return (int) (r.nextDouble() * (max - min + 1) + min);
    }

}