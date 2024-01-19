import byzcast.ByzCastClient;
import byzcast.TpccByzCastClient;
import flexcast.client.ClientAWS;
import flexcast.client.GapsClient;
import flexcast.client.TpccClient;
import skeen.SkeenClient;
import skeen.TpccSkeenClient;
import util.ArgsParser;

public class MainClient {
    public static void main(String args[]){
        ArgsParser p = ArgsParser.getClientParser(args);

        switch(p.getAlgorithm()){
            case 0: {
                if(p.isTpcc()){
                    new TpccClient(p.getId(), p);
                }
                else {
                    if(p.gapClient()){
                        new GapsClient(p.getId(), p);
                    }
                    else {
                        new ClientAWS(p.getId(), p, true);
                    }
                }
            }; break;
            case 1: {
                if(p.isTpcc())
                    new TpccSkeenClient(p.getId(), p);
                else
                    new SkeenClient(p.getId(), p, true);
                }; break;
            case 2: {
                if(p.isTpcc())
                    new TpccByzCastClient(p.getId(), p);
                else
                    new ByzCastClient(p.getId(), p, true);
            }; break;
            default: return;
        }
    }
}