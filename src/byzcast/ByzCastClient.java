package byzcast;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.stream.Collectors;
import org.jgrapht.alg.lca.TarjanLCAFinder;
import org.jgrapht.graph.DefaultEdge;
import base.Node;
import byzcast.messages.ByzCastMessage;
import byzcast.messages.ByzCastMessage.Type;
import byzcast.proxies.ByzCastClientProxy;
import util.ArgsParser;
import util.FileManager;
import util.Stats;

public class ByzCastClient extends ByzCastClientProxy {
    protected ArgsParser args;
    protected int seqNumber, totalTime;
    protected short numNodes = 0;
    protected CyclicBarrier syncAllConnections;
    protected FileManager files;
    private int localityPercentage = 0;
    private int [] destsSizes;
    protected int [][] wloadDist2dests;
    protected int [][][] wloadDist3dests;
    // protected Stats stats;
    protected final Random gen;
    private short warehouse;
    private HashMap<Short, String> nearestWHs = new HashMap<>();

    public ByzCastClient(short id, ArgsParser args, boolean start){
        super(id, args.getTree());
        this.args = args;
        totalTime = args.getDuration();
        this.files = new FileManager();
        this.localityPercentage = args.getLocality();
        this.warehouse = (short) args.getHomeWarehouse();
        this.gen = new Random(System.nanoTime());
        ArrayList<Node> nodes = files.loadHosts();
        FileManager.loadLocalityFile(nearestWHs);
        syncAllConnections = new CyclicBarrier(nodes.size()+1);
        for(Node server : nodes) connectTo(server, syncAllConnections);
        numNodes = (short) nodes.size();

        short root = 0;
        // switch(numNodes){
        //     case 9: {root = 4; break;}
        //     case 12: {root = 5; break;}
        // }
        lcafinder = new TarjanLCAFinder<Short,DefaultEdge>(tree, root);

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
        // wait all netty threads connect to all servers
        try {syncAllConnections.await();} catch(InterruptedException|BrokenBarrierException e){print("Broken barrier!!!!");}
        // send initialization message to all servers
        sendInitMessage();
        //sleep(3000);
        // send ready message to a server
        // the server will reply when all clients are ready, then we "guarantee" all clients start at (~) the same time
        sendReadyMessage();
        print("All other clients ready!");
        print("Started AWS ByzCast experiment");
        if(args.getNumPartitions() > 0) print (args.getNumPartitions(), "partitions");
        print("Locality:", localityPercentage, "%");
        print("ByzCast Tree:", args.getTree());
        print("My home warehouse:", warehouse);
        stats = new Stats(totalTime, numNodes);

        long startTime = System.nanoTime(), now;
        long elapsed = 0, usLat = startTime;

        while ((elapsed / 1e9) < totalTime) {
            ByzCastMessage m = newMessage();
            multicast(m);
            now = System.nanoTime();
            stats.store((now - usLat) / 1000, (m.getDst().length > 1));
            elapsed = (now - startTime);

            destsSizes[m.getDst().length-1]++;

            computeDistribution(m);
            
            usLat = now;
        }

        if (stats.getCount() > 0) {
            try {Files.createDirectories(Paths.get("results" + (args.getRegion().equals("") ? "" : "/"+args.getRegion())));} catch (IOException e) {}
            stats.persist("results" + (args.getRegion().equals("") ? "" : "/"+args.getRegion()) + "/" + getId() + "-stats-client-byzcast.txt", 15);
            stats.persistPerNodes("results" + (args.getRegion().equals("") ? "" : "/"+args.getRegion()) + "/" + getId() + "-stats-client-byzcast-per-node.txt", 15);
            print("LOCAL STATS:", stats);
        }

        sendEndMessage();

        for(int i = 0; i < destsSizes.length; i++) print("# of msgs to", i+1, "dests:", destsSizes[i]);

        printWloadDistribution();

        print("Finished AWS ByzCast experiment. Elapsed: ", elapsed / 1e9, "seconds");
        exit();
    }

    protected void printWloadDistribution() {
        print("Wload for destination size 2:");
        for(int i = 0; i < numNodes; i++)
            for(int j = 0; j < numNodes; j++)
                if(wloadDist2dests[i][j] > 0) print("# of msgs to [",i, j, "]:", wloadDist2dests[i][j]);

        print("Wload for destination size 3:");
        for(int i = 0; i < numNodes; i++)
            for(int j = 0; j < numNodes; j++)
                for(int k = 0; k < numNodes; k++)
                    if(wloadDist3dests[i][j][k] > 0) print("# of msgs to [",i, j, k, "]:", wloadDist3dests[i][j][k]);
    }

    protected void computeDistribution(ByzCastMessage m) {
        if (m.getDst().length == 2){
            wloadDist2dests[m.getDst()[0]][m.getDst()[1]]++;
        } else if (m.getDst().length == 3){
            wloadDist3dests[m.getDst()[0]][m.getDst()[1]][m.getDst()[2]]++;
        }
    }

    protected ByzCastMessage newMessageTo(short... dst){
        ByzCastMessage m = new ByzCastMessage(nextSeqNumber());
        m.setType(Type.MSG);
        m.setCliId(getId());
        m.setDst(dst);
        return m;
    }

    private ByzCastMessage newMessage(){
        ByzCastMessage m = new ByzCastMessage(nextSeqNumber());
        m.setType(Type.MSG);
        m.setDst(generateDests());
        m.setCliId(getId());
        return m;
    }

    private short[] generateDests(){

        if(randomNumber(1, 100, gen) <= 90){
            return new short[]{warehouse};
        }

        if(localityPercentage == 0){
            return generateRandDests();
        }
        if(randomNumber(1, 100, gen) <= localityPercentage) 
            return generate2Dests();
        return generate3Dests();
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
            tempdst[1] = getNearestWH(0);
        else 
            tempdst[1] = getNearestWH(1);

        Arrays.sort(tempdst);

        return tempdst;
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
        Arrays.sort(finaldst);

        return finaldst;
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

    // private short getNearestWH(short warehouseparam) {
    //     if(numNodes == 9){
    //         switch(warehouseparam){
    //             case 0: return 1;
    //             case 1: return 2;
    //             case 2: return 3;
    //             case 3: return 4;
    //             case 4: return 5;
    //             case 5: return 4;
    //             case 6: return 7;
    //             case 7: return 8;
    //             case 8: return 7;
    //             default: return warehouseparam;
    //         }
    //     } else if(numNodes == 12){
    //         switch(warehouseparam){
    //             case 0: return 1;
    //             case 1: return 2;
    //             case 2: return 1;
    //             case 3: return 2;
    //             case 4: return 5;
    //             case 5: return 4;
    //             case 6: return 7;
    //             case 7: return 6;
    //             case 8: return 9;
    //             case 9: return 8;
    //             case 10: return 9;
    //             case 11: return 10;
    //             default: return warehouseparam;
    //         }
    //     } else if(numNodes == 13){
    //         switch(warehouseparam){
    //             case 0: return 1;
    //             case 1: return 2;
    //             case 2: return 3;
    //             case 3: return 4;
    //             case 4: return 5;
    //             case 5: return 6;
    //             case 6: return 7;
    //             case 7: return 8;
    //             case 8: return 9;
    //             case 9: return 10;
    //             case 10: return 11;
    //             case 11: return 12;
    //             case 12: return 11;
    //             default: return warehouseparam;
    //         }
    //     }
    //     return warehouseparam;
    // }

    // private short getSecondNearestWH(short warehouseparam) {
    //     if(numNodes == 9){
    //         switch(warehouseparam){
    //             case 0: return 2;
    //             case 1: return 3;
    //             case 2: return 0;
    //             case 3: return 5;
    //             case 4: return 2;
    //             case 5: return 3;
    //             case 6: return 8;
    //             case 7: return 5;
    //             case 8: return 6;
    //             default: return warehouseparam;
    //         }
    //     } else if(numNodes == 12){
    //         switch(warehouseparam){
    //             case 0: return 2;
    //             case 1: return 3;
    //             case 2: return 0;
    //             case 3: return 1;
    //             case 4: return 6;
    //             case 5: return 7;
    //             case 6: return 4;
    //             case 7: return 5;
    //             case 8: return 10;
    //             case 9: return 11;
    //             case 10: return 8;
    //             case 11: return 9;
    //             default: return warehouseparam;
    //         }
    //     } else if(numNodes == 13){
    //         switch(warehouseparam){
    //             case 0: return 2;
    //             case 1: return 3;
    //             case 2: return 0;
    //             case 3: return 1;
    //             case 4: return 2;
    //             case 5: return 7;
    //             case 6: return 8;
    //             case 7: return 5;
    //             case 8: return 6;
    //             case 9: return 11;
    //             case 10: return 12;
    //             case 11: return 9;
    //             case 12: return 10;
    //             default: return warehouseparam;
    //         }
    //     }
    //     return warehouseparam;
    // }

    // private short getThirdNearestWH(short warehouseparam) {
    //     if(numNodes == 9){
    //         switch(warehouseparam){
    //             case 0: return 3;
    //             case 1: return 4;
    //             case 2: return 5;
    //             case 3: return 0;
    //             case 4: return 1;
    //             case 5: return 2;
    //             case 6: return 3;
    //             case 7: return 4;
    //             case 8: return 5;
    //             default: return warehouseparam;
    //         }
    //     } else if(numNodes == 12){
    //         switch(warehouseparam){
    //             case 0: return 3;
    //             case 1: return 4;
    //             case 2: return 5;
    //             case 3: return 0;
    //             case 4: return 7;
    //             case 5: return 1;
    //             case 6: return 3;
    //             case 7: return 4;
    //             case 8: return 11;
    //             case 9: return 6;
    //             case 10: return 7;
    //             case 11: return 8;
    //             default: return warehouseparam;
    //         }
    //     } else if(numNodes == 13){
    //         switch(warehouseparam){
    //             case 0: return 3;
    //             case 1: return 4;
    //             case 2: return 5;
    //             case 3: return 0;
    //             case 4: return 2;
    //             case 5: return 8;
    //             case 6: return 9;
    //             case 7: return 10;
    //             case 8: return 5;
    //             case 9: return 12;
    //             case 10: return 7;
    //             case 11: return 8;
    //             case 12: return 9;
    //             default: return warehouseparam;
    //         }
    //     }
    //     return warehouseparam;
    // }

    public static int randomNumber(int min, int max, Random r) {
        return (int) (r.nextDouble() * (max - min + 1) + min);
    }

}