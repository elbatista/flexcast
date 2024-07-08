package flexcast.reconfig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;

import base.Host;
import base.Node;
import flexcast.messages.Message.Type;
import proxies.ClientProxy;
import util.ArgsParser;
import util.FileManager;
import flexcast.messages.Message;

public class ReconfigClient extends ClientProxy {
    protected ArgsParser args;
    protected int seqNumber, totalTime, timeslice;
    protected CyclicBarrier syncAllConnections;
    protected FileManager files;
    protected final Random gen;
    private HashMap<Short, String> nearestWHs = new HashMap<>();

    public ReconfigClient(short id, ArgsParser args){
        super(id);
        this.args = args;
        totalTime = args.getDuration();
        this.files = new FileManager();
        if(!args.getLog()) setPrint(false);
        this.gen = new Random(System.nanoTime());
        ArrayList<Node> nodes = files.loadHosts();
        FileManager.loadLocalityFile(nearestWHs,0);
        syncAllConnections = new CyclicBarrier(nodes.size()+1);
        currentView = new View(0, nodes);
        connectToServers(syncAllConnections);
        this.timeslice = args.getReconfigClient()*1000;
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
        printF("Start FlexCast Reconfig Client", "["+getId()+"]");
        // wait all netty threads connect to all servers
        try {syncAllConnections.await();} 
        catch(InterruptedException|BrokenBarrierException e){printF("Broken barrier!!!!");}
        printF("Connected to all servers!");
        printF("Initial view:", currentView);
        printF("Will run for:", totalTime);
        printF("TimeSlice for check config:", timeslice);

        // send initialization message to all servers
        sendInitMessage();
        // send ready message to a server
        // the server will reply when all clients are ready, then we "guarantee" all clients start at (~) the same time
        sendReadyMessage();
        printF("All other clients ready!");

        long startTime = System.nanoTime();
        long now;
        long elapsed = 0;

        sleep(10000);

        while ((elapsed / 1e9) < totalTime) {

            sleep(timeslice);
            
            Message m = newCKMessage();
            now = System.nanoTime();
            printF("Sending request for workload data", m);
            List<Message> replies = multicast2(m);

            printF("Collected workload data", replies);
            updateDestsFreq(replies);

            printF("Calculating Optimal DAG based on workload");
            long ini = System.nanoTime();
            short[] newDAG = currentView.calculatePossibleNewDAG();
            printF("Took", TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-ini),"ms");

            if(newDAG != null){
                printF("New DAG found", Arrays.toString(newDAG));
                sendChangeViewMessage(newDAG);
            }
            else {
                printF("WLOT didnt suggest a new DAG");
            }

            // printF("CurrView", currentView.getDstsFreq());

            elapsed = (now - startTime);
        }

        sendEndMessage();            
        printF("Finished FlexCast Reconfig Client. Elapsed: ", elapsed / 1e9, "seconds");
        exit();
    }

    private void sendChangeViewMessage(short[] newDAG) {
        Message m = newCKMessage();
        m.setType(Message.Type.VIEWCHANGE);
        m.setNewOverlay(newDAG);
        printF("Multicasting change view message", m);
        multicast(m);
        printF("received all replies");
        m.setViewId(currentView.getId()+1);
        changeView(m);
    }

    private void updateDestsFreq(List<Message> replies) {
        for(Message m : replies){
            for(String key : m.getDstsFreq().keySet()){
                Integer localValue = currentView.getDstsFreq().get(key);
                if(localValue == null){
                    currentView.getDstsFreq().put(key, m.getDstsFreq().get(key));
                }
                else {
                    localValue += m.getDstsFreq().get(key);
                    currentView.getDstsFreq().put(key, localValue);
                }
            }
        }
    }

    private Message newCKMessage(){
        Message m = new Message(nextSeqNumber(), currentView.getId());
        m.setType(Type.CKFREQ);
        m.setCliId(getId());
        m.setDst(allDests());
        return m;
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
        // nextView = null;
        // process any buffered message in the new view
        printF("Changed to a new view:", currentView);
    }

    public static int randomNumber(int min, int max, Random r) {
        return (int) (r.nextDouble() * (max - min + 1) + min);
    }

}