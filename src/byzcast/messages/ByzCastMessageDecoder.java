package byzcast.messages;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;

public class ByzCastMessageDecoder extends ReplayingDecoder<ByzCastMessage> {
    private ArrayList<Double> sizes;

    public ByzCastMessageDecoder(ArrayList<Double> sizes){
        this.sizes = sizes;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        int size = in.readInt();
        byte[] data = new byte[size];
        in.readBytes(data);
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        ObjectInputStream ois = new ObjectInputStream(bais);
        try {
            ByzCastMessage m = (ByzCastMessage)ois.readObject();
            if(sizes != null && m.getType() == ByzCastMessage.Type.MSG){
                sizes.add((double)size);
            }
            out.add(m);
        }
        catch(Exception e){
            e.printStackTrace();
            System.out.println("MessageDecoder - Exception - " + e);
            System.exit(0);
        }
    }
}