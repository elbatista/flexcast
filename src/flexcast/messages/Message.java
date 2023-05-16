package flexcast.messages;

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
    private HashSet<ArrayList<LightMessage>> hst;
    private ArrayList<Short> notifList;
    
    // "transient" fields
    private Channel channelIn;

    // constructors
    public Message(){
        hst = new HashSet<>();
        notifList = new ArrayList<>();
    }

    public Message(int id){
        this.id = id;
        hst = new HashSet<>();
        notifList = new ArrayList<>();
    }

    // methods
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public short getIdNotifier() {
        return idNotifier;
    }

    public void setIdNotifier(short idNotifier) {
        this.idNotifier = idNotifier;
    }

    public ArrayList<Short> getNotifList() {
        return this.notifList;
    }

    public void setNotifList(ArrayList<Short> notifList) {
        this.notifList = notifList;
    }

    public HashSet<ArrayList<LightMessage>> getHst() {
        return hst;
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
        return toString(getId(), getType(), Arrays.toString(getDst()), getHst(), getType()==Type.ACK ? ("notifier:"+getIdNotifier()+" nl:"+getNotifList()) : "");
    }

    public boolean isAddressedTo(short d){
        for(short i : dst)
            if (i == d)
                return true;
        return false;
    }

    // Serialization methods:
    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        switch(getType()){
            case MSG: writeExtMsg(out); break;
            case ACK: writeExtAck(out); break;
            case NOTIF: writeExtNotif(out); break;
            case CONN: writeExtConn(out); break;
            case END: writeExtEnd(out); break;
            case READY: writeExtReady(out); break;
            case REPLY: writeExtReply(out); break;
        }
    }

    private void writeExtMsg(ObjectOutput out) throws IOException {
        // type
        out.writeByte(1);
        out.writeInt(getId());
        out.writeInt(getCliId());
        out.writeShort(getSender());
        //out.writeInt(getNotifsSent());
        writeExtDsts(out);
        writeExtHst(out);
        writeExtNotifList(out);
    }

    private void writeExtNotifList(ObjectOutput out) throws IOException{
        if(getNotifList() == null) {
            out.writeShort(0);
        }
        else {
            out.writeShort(getNotifList().size());
            for(short n : getNotifList()){
                out.writeShort(n);
            }
        }
    }

    private void writeExtDsts(ObjectOutput out) throws IOException {
        out.writeShort(getDst().length);
        for(int i = 0; i < getDst().length; i++){
            out.writeShort(getDst()[i]);
        }
    }

    private void writeExtDsts(ObjectOutput out, LightMessage lm) throws IOException {
        out.writeShort(lm.getDst().length);
        for(int i = 0; i < lm.getDst().length; i++){
            out.writeShort(lm.getDst()[i]);
        }
    }

    private void writeExtHst(ObjectOutput out) throws IOException {
        // how many arrays in the set:
        out.writeInt(getHst().size());
        for(ArrayList<LightMessage> hst : getHst()){
            // how many msgs in the array
            out.writeInt(hst.size());
            for(LightMessage lm : hst){
                out.writeInt(lm.getId());
                writeExtDsts(out, lm);
            }
        }
    }

    private void writeExtAck(ObjectOutput out) throws IOException {
        // type
        out.writeByte(2);
        out.writeInt(getId());
        out.writeShort(getSender());
        out.writeShort(getIdNotifier());
        writeExtDsts(out);
        writeExtHst(out);
        writeExtNotifList(out);
    }

    private void writeExtNotif(ObjectOutput out) throws IOException {
        // type
        out.writeByte(3);
        out.writeInt(getId());
        out.writeShort(getSender());
        writeExtDsts(out);
        writeExtHst(out);
        writeExtNotifList(out);
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
            case 1: readExtMsg(in); break;
            case 2: readExtAck(in); break;
            case 3: readExtNotif(in); break;
            case 4: readExtConn(in); break;
            case 5: readExtReply(in); break;
            case 9: readExtReady(in); break;
            case 10: readExtEnd(in); break;
        }
    }

    private void readExtMsg(ObjectInput in) throws IOException {
        setType(Type.MSG);
        setId(in.readInt());
        setCliId(in.readInt());
        setSender(in.readShort());
        readExtDsts(in);
        readExtHst(in);
        readExtNotifList(in);
    }

    private void readExtNotifList(ObjectInput in) throws IOException {
        short size = in.readShort();
        for(short i = 0; i < size; i++){
            getNotifList().add(in.readShort());
        }
    }

    private void readExtDsts(ObjectInput in) throws IOException {
        short dstSize = in.readShort();
        dst = new short[dstSize];
        for(int i = 0; i < dstSize; i++){
            dst[i] = in.readShort();
        }
    }

    private void readExtDsts(ObjectInput in, LightMessage lm) throws IOException {
        short dstSize = in.readShort();
        short [] tmpdst = new short[dstSize];
        for(int i = 0; i < dstSize; i++){
            tmpdst[i] = in.readShort();
        }
        lm.setDst(tmpdst);
    }

    private void readExtHst(ObjectInput in) throws IOException {
        // read how many arrays
        int qty = in.readInt();
        for(int i  = 0; i < qty; i++){
            // read the size of the array
            int size = in.readInt();
            ArrayList<LightMessage> array = new ArrayList<>();
            for(int j = 0; j < size; j++){
                LightMessage lm = new LightMessage(in.readInt());
                readExtDsts(in, lm);
                array.add(lm);
            }
            if(array.size() > 0) getHst().add(array);
        }
    }

    private void readExtAck(ObjectInput in) throws IOException {
        setType(Type.ACK);
        setId(in.readInt());
        setSender(in.readShort());
        setIdNotifier(in.readShort());
        readExtDsts(in);
        readExtHst(in);
        readExtNotifList(in);
    }

    private void readExtNotif(ObjectInput in) throws IOException {
        setType(Type.NOTIF);
        setId(in.readInt());
        setSender(in.readShort());
        readExtDsts(in);
        readExtHst(in);
        readExtNotifList(in);
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
