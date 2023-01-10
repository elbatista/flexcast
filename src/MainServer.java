import core.ServerNodeFunctions;
import skeen.SkeenNode;
import util.ArgsParser;

public class MainServer {
    public static void main(String args[]){
        ArgsParser p = ArgsParser.getServerParser(args);
        if(p.isSkeen()){
            new SkeenNode(p.getId(), p);
        }
        else {
            new ServerNodeFunctions(p.getId(), p);
        }
    }
}