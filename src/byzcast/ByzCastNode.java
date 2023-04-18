package byzcast;

import java.util.ArrayList;
import java.util.List;
import org.javatuples.Pair;
import util.ArgsParser;
import util.FileManager;
import base.Host;
import base.Node;
import byzcast.messages.ByzCastMessage;
import byzcast.proxies.ByzCastServerProxy;
import messages.LightMessage;
import messages.LightMessagesList;

public class ByzCastNode extends ByzCastServerProxy {
    protected int numNodes;
    protected FileManager files;
    private LightMessagesList history = new LightMessagesList();
    private List<Node> children = new ArrayList<>();
    
    public ByzCastNode(short id, ArgsParser args){
        super(id, args.getClientCount());
        this.files = new FileManager();
        List<Node> nodes = files.loadHosts();
        numNodes = nodes.size();
        Host thisHost = null;
        for(Node n : nodes){
            if(n.getId() == id){
                thisHost = n.getHost();
                break;
            }
        }
        setHost(thisHost);
        print(this, "ByzCast Node - Start listening ...");
        // sets connection to all children nodes
        // para cada pair no arquivo de config do byzcast
        // se o pair comeca com o meu id, conecto no node correspondente ao segundo id
        for(Pair<Short, Short> p : files.loadByzCastTree()){
            if (p.getValue0() == getId()){
                for(Node node : nodes){
                    if(p.getValue1() == node.getId()){
                        connectTo(node);
                        children.add(node);
                    }
                }
            }
        }
        // for(Node node : nodes)
        //     connectTo(node);
    }

    @Override
    protected void receiveMsg(ByzCastMessage m){
        // print("Received message", m);

        for(Node n : children){
            send(m, n.getId());
            // print("Fwd message", m, "to", n.getId());
        }

        if(m.isAddressedTo(getId())){
            deliver(m);
        }
    }

    private void deliver(ByzCastMessage m) {
        history.add(new LightMessage(m.getId(), m.getDst()));
        sendReply(m);
        // print("Delivered message", m);
    }

    protected void finish(){
        if(bufferQueue.size() > 0){
            print("Queue is not empty !!! ");
            files.stop();
            exit();
        }
        print("Queue is empty ! =]");
        files.persistMessages(history, getId(), false, false);
        print("-------------------------------------");
        print("Total msgs in the history:", history.size());
        print("Total local msgs received:", localMsgs);
        print("-------------------------------------");
        files.nodeFinished(getId());
        exit();
    }
}
