package flexcast.messages;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import org.javatuples.Pair;
import io.netty.channel.Channel;
import util.BaseObj;

public class Message extends BaseObj implements Externalizable {
    public enum Type {MSG, ACK, NOTIF, CONN, REPLY, END, READY, GC}
    private short sender = -1, idNotifier = -1;
    private int id = -1, cliId = -1;
    private Type type;
    private short [] dst;
    private HashMap<Short, LightMessagesList> hst;
    private ArrayList<Pair<Short, Integer>> notifList;
    private int idNotif=-1;
    
    // "transient" fields
    private Channel channelIn;
    private HashSet<Integer> pendNotifOrigins;

    // constructors
    public Message(){
        hst = new HashMap<>();
        notifList = new ArrayList<>();
    }

    public Message(int id){
        this.id = id;
        hst = new HashMap<>();
        notifList = new ArrayList<>();
    }

    // methods
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public HashSet<Integer> getPendNotifOrigins() {
        if(pendNotifOrigins == null) pendNotifOrigins = new HashSet<>();
        return pendNotifOrigins;
    }
    
    public int getIdNotif() {
        return idNotif;
    }

    public void setIdNotif(int idNotif) {
        this.idNotif = idNotif;
    }

    public short getIdNotifier() {
        return idNotifier;
    }

    public void setIdNotifier(short idNotifier) {
        this.idNotifier = idNotifier;
    }

    public ArrayList<Pair<Short, Integer>> getNotifList() {
        return this.notifList;
    }

    public void setNotifList(ArrayList<Pair<Short, Integer>> notifs) {
        for(Pair<Short, Integer> p : notifs)
            this.notifList.add(p);
    }

    public HashMap<Short, LightMessagesList> getHst() {
        return hst;
    }

    public void addHst(short anc, ArrayList<LightMessage> hstParam) {
        LightMessagesList list = getHst().get(anc);
        if(list == null){
            list = new LightMessagesList();
            getHst().put(anc, list);
        }
        for(LightMessage lm : hstParam){
            list.add(lm);
        }
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
    public int hashCode() {
        return getId();
    }
    
    @Override
    public boolean equals(Object m){
        return ((Message)m).getId() == getId();
    }

    public String toString(){
        return toString(getId(), getType(), Arrays.toString(getDst()), getHst(), "notifier:", getIdNotifier(), " nl:", getNotifList());
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
            case GC: writeExtGC(out); break;
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
            for(Pair<Short, Integer> p : getNotifList()){
                out.writeShort(p.getValue0());
                out.writeInt(p.getValue1());
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
        // how many lists in the set:
        out.writeShort(getHst().size());
        for(short anc : getHst().keySet()){
            out.writeShort(anc);
            // write how many msgs in the list
            out.writeInt(getHst().get(anc).size());

            for(LightMessage lm :  getHst().get(anc)){
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
        out.writeInt(getIdNotif());
        writeExtDsts(out);
        writeExtHst(out);
        writeExtNotifList(out);
    }

    private void writeExtNotif(ObjectOutput out) throws IOException {
        // type
        out.writeByte(3);
        out.writeInt(getId());
        out.writeShort(getSender());
        out.writeInt(getIdNotif());
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
    
    private void writeExtGC(ObjectOutput out) throws IOException {
        out.writeByte(6);
        out.writeInt(getId());
        out.writeInt(getCliId());
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
            case 6: readExtGC(in); break;
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
            short d = in.readShort();
            int idNt = in.readInt();
            getNotifList().add(new Pair<Short,Integer>(d, idNt));
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
        // read how many lists
        short qty = in.readShort();
        for(int i  = 0; i < qty; i++){
            // read the respective ancestor
            short anc = in.readShort();
            // read the size of the list
            int size = in.readInt();
            LightMessagesList list = new LightMessagesList();
            for(int j = 0; j < size; j++){
                LightMessage lm = new LightMessage(in.readInt());
                readExtDsts(in, lm);
                list.add(lm);
            }
            getHst().put(anc, list);
        }
    }

    private void readExtAck(ObjectInput in) throws IOException {
        setType(Type.ACK);
        setId(in.readInt());
        setSender(in.readShort());
        setIdNotifier(in.readShort());
        setIdNotif(in.readInt());
        readExtDsts(in);
        readExtHst(in);
        readExtNotifList(in);
    }

    private void readExtNotif(ObjectInput in) throws IOException {
        setType(Type.NOTIF);
        setId(in.readInt());
        setSender(in.readShort());
        setIdNotif(in.readInt());
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

    private void readExtGC(ObjectInput in) throws IOException {
        setType(Type.GC);
        setId(in.readInt());
        setCliId(in.readInt());
        setSender(in.readByte());
    }

}
