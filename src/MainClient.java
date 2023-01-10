import core.Client;
import skeen.SkeenClient;
import skeen.TpccSkeenClient;
import util.ArgsParser;
import core.TpccClient;

public class MainClient {
    public static void main(String args[]){
        ArgsParser p = ArgsParser.getClientParser(args);
        if(p.isSkeen()){
            if(p.isTpcc())
                new TpccSkeenClient(p.getId(), p);
            else
                new SkeenClient(p.getId(), p, true);
        }
        else {
            if(p.isTpcc())
                new TpccClient(p.getId(), p);
            else
                new Client(p.getId(), p, true);
        }
    }
}