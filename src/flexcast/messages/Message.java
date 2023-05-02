package flexcast.messages;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import io.netty.channel.Channel;
import util.BaseObj;

public class Message extends BaseObj implements Externalizable {
    public enum Type {MSG, ACK, CONN, REPLY, END, READY}
    private short sender = -1;
    private int id = -1, cliId = -1;
    private Type type;
    private short [] dst;
    private HashMap<Short, ArrayList<LightMessage>> hst = new HashMap<>();
    
    // "transient" fields
    private Channel channelIn;
    private int acksFromAncsDeps=0;
    private HashSet<Integer> msgDeps = new HashSet<>();
    private HashMap<Short, HashMap<Integer, Boolean>> ancHstMsgsDelivered = new HashMap<>();
    private boolean updtOrigin = false;

    public boolean alreadyUpdtOrigin() {
        return updtOrigin;
    }

    public void alreadyUpdtOrigin(boolean updtOrigin) {
        this.updtOrigin = updtOrigin;
    }

    public HashSet<Integer> getMsgDeps() {
        return msgDeps;
    }

    public HashMap<Short, ArrayList<LightMessage>> getHst() {
        return hst;
    }

    public int getAcksFromAncsDeps() {
        return acksFromAncsDeps;
    }

    public void addAckFromAncDep(){
        acksFromAncsDeps++;
    }

    public void recvAckFromAnc(){
        acksFromAncsDeps--;
    }

    // constructors
    public Message(){}
    public Message(int id){
        this.id = id;
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

    public int getCliId() {
        return cliId;
    }

    public void setCliId(int cliId) {
        this.cliId = cliId;
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

    @Override
    public boolean equals(Object m){
        return ((Message)m).getId() == getId();
    }

    public String toString(){
        return toString(getId(), getType(), Arrays.toString(getDst()), getHst());
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

        // hst
        out.writeShort((short) getHst().size());
        for(short anc : getHst().keySet()){
            out.writeShort(anc);
            out.writeInt(getHst().get(anc).size());
            for(LightMessage lm : getHst().get(anc)){
                out.writeInt(lm.getId());
                out.writeByte(lm.getDst().length);
                for(short d : lm.getDst()) out.writeByte(d);
            }
        }
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

        // hst
        out.writeShort((short) getHst().size());
        for(short anc : getHst().keySet()){
            out.writeShort(anc);
            out.writeInt(getHst().get(anc).size());
            for(LightMessage lm : getHst().get(anc)){
                out.writeInt(lm.getId());
                out.writeByte(lm.getDst().length);
                for(short d : lm.getDst()) out.writeByte(d);
            }
        }
        

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
            case 4: readExtConn(in); break;
            case 5: readExtReply(in); break;
            case 9: readExtReady(in); break;
            case 10: readExtEnd(in); break;
        }
    }

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


        //hst
        short hstSize = in.readShort();
        for(short i = 0; i < hstSize; i++){
            short anc = in.readShort();
            int nMsgs = in.readInt();
            ArrayList<LightMessage> msgs = new ArrayList<>();
            for(int j = 0; j < nMsgs; j++){
                LightMessage lm = new LightMessage(in.readInt());
                dstLen = in.readByte();
                short[] lmDsts = new short[dstLen];
                for(int k = 0; k < dstLen; k++) lmDsts[k] = in.readByte();
                lm.setDst(lmDsts);
                msgs.add(lm);
            }
            getHst().put(anc, msgs);
        }
        

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

        //hst
        short hstSize = in.readShort();
        for(short i = 0; i < hstSize; i++){
            short anc = in.readShort();
            int nMsgs = in.readInt();
            ArrayList<LightMessage> msgs = new ArrayList<>();
            for(int j = 0; j < nMsgs; j++){
                LightMessage lm = new LightMessage(in.readInt());
                dstLen = in.readByte();
                short[] lmDsts = new short[dstLen];
                for(int k = 0; k < dstLen; k++) lmDsts[k] = in.readByte();
                lm.setDst(lmDsts);
                msgs.add(lm);
            }
            getHst().put(anc, msgs);
        }

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

    public void updateAncHst(Message m) {
        for(short anc : m.getHst().keySet()){
            for(LightMessage lm : m.getHst().get(anc)){
                if(ancHstMsgsDelivered.get(anc) == null) ancHstMsgsDelivered.put(anc, new HashMap<>());
                if(ancHstMsgsDelivered.get(anc).get(lm.getId()) == null){
                    if(getHst().get(anc)==null) getHst().put(anc, new ArrayList<>());
                    getHst().get(anc).add(lm);
                    ancHstMsgsDelivered.get(anc).put(lm.getId(), true);
                }
            }
        }
    }

    public void updateDeps(short nodeid, HashMap<Integer, Boolean> msgsDelivered, HashMap<Integer, HashSet<Message>> msgsThatAreDeps) {
        // crio uma pendencia de msg para cada msg anterior a m addr pra mim que esta no hst atualizado dessa msg q eu nao entreguei ainda
        for(short anc : getHst().keySet()){
            for(LightMessage lm : getHst().get(anc)){
                if(lm.getId() == getId()) break;
                if(lm.isAddressedTo(nodeid) && msgsDelivered.get(lm.getId()) == null){
                    getMsgDeps().add(lm.getId());
                    if(msgsThatAreDeps.get(lm.getId()) == null){
                        msgsThatAreDeps.put(lm.getId(), new HashSet<>());
                    }
                    msgsThatAreDeps.get(lm.getId()).add(this);
                }
            }
        }
    }

    public void updateDeps(short nodeid, HashMap<Integer, Boolean> msgsDelivered, HashMap<Integer, HashSet<Message>> msgsThatAreDeps, HashMap<Short, ArrayList<LightMessage>> hstAck) {
        // crio uma pendencia de msg para cada msg anterior a m addr pra mim que esta no hst do ack por paramero
        for(short anc : hstAck.keySet()){
            for(LightMessage lm : hstAck.get(anc)){
                if(lm.getId() == getId()) break;
                if(lm.isAddressedTo(nodeid) && msgsDelivered.get(lm.getId()) == null){
                    getMsgDeps().add(lm.getId());
                    if(msgsThatAreDeps.get(lm.getId()) == null){
                        msgsThatAreDeps.put(lm.getId(), new HashSet<>());
                    }
                    msgsThatAreDeps.get(lm.getId()).add(this);
                }
            }
        }
    }

}
