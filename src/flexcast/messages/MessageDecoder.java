package flexcast.messages;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;
import util.MsgSize;

public class MessageDecoder extends ReplayingDecoder<Message> {
    private ArrayList<MsgSize> sizes;

    public MessageDecoder(ArrayList<MsgSize> sizes){
        this.sizes = sizes;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        int size = in.readInt();
        byte[] data = new byte[size];
        in.readBytes(data);
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        ObjectInputStream ois = new ObjectInputStream(bais);
        Message m = (Message) ois.readObject();
        if(sizes != null && m.getType() == Message.Type.MSG || m.getType() == Message.Type.ACK || m.getType() == Message.Type.NOTIF){
            sizes.add(new MsgSize(System.nanoTime(), m.getId(), (double)size, m.getDst()));
        }
        out.add(m);
    }
}