package byzcast;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    private ArrayList<String[]> mappings = new ArrayList<>();
    
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
        for(Pair<Short, Short> p : files.loadByzCastTree(mappings, getId())){
            if (p.getValue0() == getId()){
                for(Node node : nodes){
                    if(p.getValue1() == node.getId()){
                        connectTo(node);
                        children.add(node);
                    }
                }
            }
        }
    }

    @Override
    protected void receiveMsg(ByzCastMessage m){
        // print("Received message", m);

        Set<Short> sent = new HashSet<>();

        // send to its children
        for(Node n : children){
            if(m.isAddressedTo(n.getId())){
                send(m, n.getId());
                sent.add(n.getId());
                // print("Send to child", n.getId());
            }
        }

        // for each mapping, send to the children in the mapping, if not sent yet
        for(String[] map : mappings){
            if(m.isAddressedTo(Short.valueOf(map[1])) && !sent.contains(Short.valueOf(map[2]))){
                send(m, Short.valueOf(map[2]));
                sent.add(Short.valueOf(map[2]));
                // print("Send to child",Short.valueOf(map[2]), "via mapping, for node", Short.valueOf(map[1]));
            }
        }

        if(m.isAddressedTo(getId())){
            deliver(m);
            // print("Delivered message", m);
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
