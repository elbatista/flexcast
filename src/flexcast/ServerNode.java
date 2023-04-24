package flexcast;

import proxies.ServerProxy;
import util.FileManager;
import base.Node;
import messages.Message;

public abstract class ServerNode extends ServerProxy {
    protected FileManager files;
    private short numNodes; // num of nodes


    public ServerNode(short id, int numClients){
        super(id, numClients);
        this.files = new FileManager();
        for(Node n : files.loadHosts()){
            if(n.getId() == id) setHost(n.getHost());
            // sets connection to each descendant
            if(n.getId() > id) connectTo(n);
            numNodes++;
        }
        print(this, "FlexCast - Start listening...");
    }

    public short getNumNodes() {
        return numNodes;
    }

    @Override
    protected void receiveMsg(Message m){
        print("Received", m);
        deliver(m);
    }

    @Override
    protected void receiveAck(Message ack){
        
    }
    
    @Override
    protected void receiveNotif(Message notif){
        
    }

    protected void finish(){
        
        files.nodeFinished(getId());
        exit();
    }
    
    abstract void sendAck(Message m);
    abstract void deliver(Message m);
}