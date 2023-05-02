import byzcast.ByzCastNode;
import flexcast.FlexCastFunctions;
import skeen.SkeenNode;
import util.ArgsParser;

public class MainServer {
    public static void main(String args[]){
        ArgsParser p = ArgsParser.getServerParser(args);
        switch(p.getAlgorithm()){
            case 0: new FlexCastFunctions(p.getId(), p); break;     // FLEXCAST
            case 1: new SkeenNode(p.getId(), p); break;             // SKEEN
            case 2: new ByzCastNode(p.getId(), p); break;           // BYZCAST
            default: return;
        }
    }
}