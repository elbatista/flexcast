package flexcast;

import messages.Message;
import util.ArgsParser;

public class FlexCastNode extends ServerNode {

    public FlexCastNode(short id, ArgsParser args) {
        super(id, args.getClientCount());
    }


    @Override
    protected void deliver(Message m) {
        print("delivered", m);
        forward(m);
        sendReply(m);
    }

    private void forward(Message m) {
        for(short d: m.getDst()){
            send(m, d);
        }
    }

    @Override
    protected void sendAck(Message m) {
        
    }

}