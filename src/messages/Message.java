package messages;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import io.netty.channel.Channel;
import util.BaseObj;

public class Message extends BaseObj implements Externalizable {
    public enum Type {MSG, ACK, NOTIF, CONN, REPLY, END, READY}
    private short sender = -1, idNotifier = -1;
    private int id = -1, cliId = -1;
    private Type type;
    private short [] dst;
    private boolean ackIsFromDst = false;

    // "transient" fields
    private ArrayList<Message> acks = new ArrayList<>();
    private Channel channelIn;
    private HashSet<Integer> pendNotifOrigins;

    // constructor
    public Message(){
        this.acks = new ArrayList<>();
    }

    public Message(int id){
        this.id = id;
        this.acks = new ArrayList<>();
    }

    // methods
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Channel getChannelIn() {
        return channelIn;
    }

    public void setChannelIn(Channel channelIn) {
        this.channelIn = channelIn;
    }

    public boolean ackIsFromDst() {
        return this.ackIsFromDst;
    }

    public void ackIsFromDst(boolean ackIsFromDst) {
        this.ackIsFromDst = ackIsFromDst;
    }

    public short getIdNotifier() {
        return idNotifier;
    }

    public void setIdNotifier(short idNotifier) {
        this.idNotifier = idNotifier;
    }

    public int getCliId() {
        return cliId;
    }

    public void setCliId(int cliId) {
        this.cliId = cliId;
    }

    public ArrayList<Message> getAcks() {
        if(this.acks == null) this.acks = new ArrayList<>();
        return this.acks;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public short getSender() {
        return sender;
    }

    public void setSender(short sender) {
        this.sender = sender;
    }

    public short[] getDst() {
        return dst;
    }

    public void setDst(short... dst) {
        this.dst = dst;
    }

    public short getLca(){
        return this.dst[0];
    }

    public HashSet<Integer> getPendNotifOrigins() {
        if(pendNotifOrigins == null) pendNotifOrigins = new HashSet<>();
        return pendNotifOrigins;
    }

    @Override
    public boolean equals(Object m){
        return ((Message)m).getId() == getId();
    }

    public String toString(){
        return toString(getId(), getType(), Arrays.toString(getDst()));
    }

    public boolean isAddressedTo(short d){
        for(short i : dst)
            if (i == d)
                return true;
        return false;
    }

    public short getLcd(LightMessage m){
        short lcd = -1;
        for (short dst1 : getDst()) {
            for(short dst2 : m.getDst()){
                if(dst1 < dst2) break;
                if(dst1 == dst2) return dst1;
            }
        }
        return lcd;
    }

    public short getLcd(Message m){
        short lcd = -1;
        for (short dst1 : getDst()) {
            for(short dst2 : m.getDst()){
                if(dst1 < dst2) break;
                if(dst1 == dst2) return dst1;
            }
        }
        return lcd;
    }


    // Serialization methods:
    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        switch(getType()){
            case MSG: writeExtMsg(this, out, false); break;
            case ACK: writeExtAck(this, out, false); break;
            case NOTIF: writeExtNotif(this, out, false); break;
            case CONN: writeExtConn(out); break;
            case END: writeExtEnd(out); break;
            case READY: writeExtReady(out); break;
            case REPLY: writeExtReply(out); break;
        }
    }

    private void writeExtMsg(Message msg, ObjectOutput out, boolean batch) throws IOException {
        // type
        out.writeByte(1);

        // msg id
        out.writeInt(msg.getId());

        // client id
        out.writeInt(msg.getCliId());

        // sender
        out.writeByte(msg.getSender());

        // dests
        out.writeByte(msg.getDst().length);
        for(short i : msg.getDst()) out.writeByte(i);

    }

    private void writeExtAck(Message ack, ObjectOutput out, boolean batch) throws IOException {
        // type
        out.writeByte(2);

        // msg id
        out.writeInt(ack.getId());

        // sender
        out.writeByte(ack.getSender());

        // dests
        out.writeByte(ack.getDst().length);
        for(short i : ack.getDst()) out.writeByte(i);

        // idNotifier
        out.writeByte(ack.getIdNotifier());

        // flag ack-is-from-dst
        out.writeBoolean(ack.ackIsFromDst());

    }

    private void writeExtNotif(Message notif, ObjectOutput out, boolean batch) throws IOException {
        // type
        out.writeByte(3);

        // msg id
        out.writeInt(notif.getId());
        
        // sender
        out.writeByte(notif.getSender());

        // dests
        out.writeByte(notif.getDst().length);
        for(short i : notif.getDst()) out.writeByte(i);

    }

    private void writeExtConn(ObjectOutput out) throws IOException {
        out.writeByte(4);
        out.writeInt(getCliId());
        out.writeByte(getSender());
    }
    
    private void writeExtReady(ObjectOutput out) throws IOException {
        out.writeByte(9);
        out.writeInt(getCliId());
        out.writeByte(getSender());
    }
    
    private void writeExtEnd(ObjectOutput out) throws IOException {
        out.writeByte(10);
        out.writeInt(getCliId());
        out.writeByte(getSender());
    }
    
    private void writeExtReply(ObjectOutput out) throws IOException {
        out.writeByte(5);
        out.writeByte(getSender());
    }
    
    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        short type = in.readByte();
        switch(type){
            case 1: readExtMsg(this, in); break;
            case 2: readExtAck(this, in); break;
            case 3: readExtNotif(this, in); break;
            case 4: readExtConn(in); break;
            case 5: readExtReply(in); break;
            case 9: readExtReady(in); break;
            case 10: readExtEnd(in); break;
        }
    }

    // private void readExtBatch(ObjectInput in) throws IOException {
    //     setType(Type.BATCH);
    //     setSender(in.readByte());
    //     int size = in.readInt();

    //     for(int i = 0; i < size; i++){
    //         Message m = new Message();
    //         short type = in.readByte();
    //         switch (type){
    //             case 1: readExtMsg(m, in); break;
    //             case 2: readExtAck(m, in); break;
    //             case 3: readExtNotif(m, in); break;
    //             default: break;
    //         }
    //         getBatch().add(m);
    //     }

    //     setDepGraph(new MessageDepGraph((short)(getSender()+1)));
    //     for(int i = 0; i <= getSender(); i++){
    //         int listSize = in.readInt();
    //         for(int j = 0; j < listSize; j++){
    //             LightMessage lm = new LightMessage(in.readInt());
    //             short dstLen = in.readByte();
    //             short[] lmDsts = new short[dstLen];
    //             for(int k = 0; k < dstLen; k++) lmDsts[k] = in.readByte();
    //             lm.setDst(lmDsts);
    //             getDepGraph().add(lm, (short) i);
    //         }
    //     }
    // }

    private void readExtMsg(Message msg, ObjectInput in) throws IOException {
        msg.setType(Type.MSG);

        // msg id
        msg.setId(in.readInt());
        
        // client id
        msg.setCliId(in.readInt());
        
        // sender
        msg.setSender(in.readByte());

        // dests
        short dstLen = in.readByte();
        short [] dstAux = new short[dstLen];
        for(short i = 0; i < dstLen; i++) dstAux[i] = in.readByte();
        msg.setDst(dstAux);

    }

    private void readExtAck(Message ack, ObjectInput in) throws IOException {
        ack.setType(Type.ACK);

        // msg id
        ack.setId(in.readInt());
        
        // sender
        ack.setSender(in.readByte());

        // dests
        short dstLen = in.readByte();
        short [] dstAux = new short[dstLen];
        for(short i = 0; i < dstLen; i++) dstAux[i] = in.readByte();
        ack.setDst(dstAux);

        // idNotifier
        ack.setIdNotifier(in.readByte());

        // flag ack-is-from-dst
        ack.ackIsFromDst(in.readBoolean());

    }

    private void readExtNotif(Message notif, ObjectInput in) throws IOException {
        notif.setType(Type.NOTIF);

        // msg id
        notif.setId(in.readInt());
                
        // sender
        notif.setSender(in.readByte());

        // dests
        short dstLen = in.readByte();
        short [] dstAux = new short[dstLen];
        for(short i = 0; i < dstLen; i++) dstAux[i] = in.readByte();
        notif.setDst(dstAux);

    }

    private void readExtConn(ObjectInput in) throws IOException {
        setType(Type.CONN);
        setCliId(in.readInt());
        setSender(in.readByte());
    }

    private void readExtReady(ObjectInput in) throws IOException {
        setType(Type.READY);
        setCliId(in.readInt());
        setSender(in.readByte());
    }

    private void readExtEnd(ObjectInput in) throws IOException {
        setType(Type.END);
        setCliId(in.readInt());
        setSender(in.readByte());
    }

    private void readExtReply(ObjectInput in) throws IOException {
        setType(Type.REPLY);
        setSender(in.readByte());
    }

}
