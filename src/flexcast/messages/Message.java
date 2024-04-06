package flexcast.messages;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import org.javatuples.Pair;
import io.netty.channel.Channel;
import util.BaseObj;
import util.OrderItem;

public class Message extends BaseObj implements Externalizable {

    public enum Type {MSG, ACK, NOTIF, CONN, REPLY, END, READY, GC, VIEWCHANGE, CKFREQ}
    public enum TransactionType {NEW, PAYMENT, STATUS, DELIVERY, STOCK, NOPAYLOAD}
    private short sender = -1, idNotifier = -1;
    private int id = -1, cliId = -1;
    private Type type;
    private int viewid;
    private short [] dst;
    private HashMap<Short, LightMessagesList> hst;
    private ArrayList<Pair<Short, Integer>> notifList;
    private int idNotif=-1;

    // used in viewchange msgs
    private short [] newOverlay;
    private HashMap<String, Integer> dstsFreq;

    //payload fields
    private TransactionType transaction;
    private Date orderDate;

    //neworder
    private ArrayList<OrderItem> items;

    public void setItems(ArrayList<OrderItem> items) {
        this.items = items;
    }

    //payment
    private double paymentAmount;

    //delivery and stocklevel
    private int carrierid_or_threshold;
    
    // "transient" fields
    private Channel channelIn;
    private HashSet<Integer> pendNotifOrigins;

    // constructors
    public Message(){
        items = new ArrayList<>();
        hst = new HashMap<>();
        notifList = new ArrayList<>();
        dstsFreq = new HashMap<>();
    }

    public Message(int id, int viewid){
        this();
        this.id = id;
        this.viewid = viewid;
        // notifList = new ArrayList<>();
    }

    // methods
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getViewId() {
        return viewid;
    }

    public void setViewId(int id) {
        this.viewid = id;
    }

    public HashMap<String, Integer> getDstsFreq() {
        return dstsFreq;
    }

    public void setDstsFreq(HashMap<String, Integer> dstsFreq) {
        this.dstsFreq = dstsFreq;
    }

    public Date getOrderDate() {
        return orderDate;
    }

    public double getPaymentAmount() {
        return paymentAmount;
    }

    public int getCarrierid_or_threshold() {
        return carrierid_or_threshold;
    }
    public TransactionType getTransaction() {
        return transaction;
    }

    public void setTransaction(TransactionType transaction) {
        this.transaction = transaction;
    }

    public ArrayList<OrderItem> getItems() {
        return items;
    }

    public void setOrderDate(Date orderDate) {
        this.orderDate = orderDate;
    }

    public void setPaymentAmount(double paymentAmount) {
        this.paymentAmount = paymentAmount;
    }

    public void setCarrierid_or_threshold(int carrierid_or_threshold) {
        this.carrierid_or_threshold = carrierid_or_threshold;
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

    public short[] getNewOverlay() {
        return newOverlay;
    }

    public void setNewOverlay(short[] newOverlay) {
        this.newOverlay = newOverlay;
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
        return toString(
            getId(), 
            getViewId(), 
            getType(), 
            Arrays.toString(getDst()), 
            // getHst(), 
            // "notifier:", getIdNotifier(), 
            // " nl:", getNotifList(),
            "dstFreq:", dstsFreq
        );
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
            case VIEWCHANGE: writeExtViewChange(out); break;
            case CKFREQ: writeExtDestsFreq(out); break;
        }
    }

    private void writeExtMsg(ObjectOutput out) throws IOException {
        // type
        out.writeByte(1);
        out.writeInt(getId());
        out.writeInt(getViewId());
        out.writeInt(getCliId());
        out.writeShort(getSender());
        //out.writeInt(getNotifsSent());
        writeExtDsts(out);
        writeExtHst(out);
        writeExtNotifList(out);
        writeExtPayload(out);
    }

    private void writeExtPayload(ObjectOutput out) throws IOException{
        switch(transaction){
            case NOPAYLOAD: {out.writeByte(10); return;}
            case NEW: {
                out.writeByte(1); 
                out.writeInt(items.size());
                for(OrderItem item : items){
                    out.writeInt(item.getId());
                    out.writeInt(item.getQty());
                }
                break;
            }
            case PAYMENT: {
                out.writeByte(2); 
                out.writeDouble(paymentAmount);
                break;
            }
            case STATUS: {
                out.writeByte(3);
                break;
            }
            case DELIVERY: {
                out.writeByte(4); 
                out.writeInt(carrierid_or_threshold);
                break;
            }
            case STOCK: {
                out.writeByte(5); 
                out.writeInt(carrierid_or_threshold);
                break;
            }
        }
        out.writeLong(orderDate.getTime());
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
        out.writeInt(getViewId());
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
        out.writeInt(getViewId());
        out.writeShort(getSender());
        out.writeInt(getIdNotif());
        writeExtDsts(out);
        writeExtHst(out);
        writeExtNotifList(out);
    }

    private void writeExtConn(ObjectOutput out) throws IOException {
        out.writeByte(4);
        out.writeInt(getViewId());
        out.writeInt(getCliId());
        out.writeByte(getSender());
    }
    
    private void writeExtReady(ObjectOutput out) throws IOException {
        out.writeByte(9);
        out.writeInt(getViewId());
        out.writeInt(getCliId());
        out.writeByte(getSender());
    }
    
    private void writeExtEnd(ObjectOutput out) throws IOException {
        out.writeByte(10);
        out.writeInt(getViewId());
        out.writeInt(getCliId());
        out.writeByte(getSender());
    }
    
    private void writeExtReply(ObjectOutput out) throws IOException {
        out.writeByte(5);
        out.writeInt(getViewId());
        out.writeByte(getSender());
    }
    
    private void writeExtGC(ObjectOutput out) throws IOException {
        out.writeByte(6);
        out.writeInt(getId());
        out.writeInt(getViewId());
        out.writeInt(getCliId());
        out.writeByte(getSender());
    }

    private void writeExtViewChange(ObjectOutput out) throws IOException {
        out.writeByte(7);
        out.writeInt(getId());
        out.writeInt(getViewId());
        out.writeInt(getCliId());
        out.writeByte(getSender());
        out.writeShort(getNewOverlay().length);
        for(short i : getNewOverlay()){
            out.writeShort(i);
        }
        writeExtDsts(out);
        writeExtHst(out);
        writeExtNotifList(out);
    }

    private void writeExtDestsFreq(ObjectOutput out) throws IOException {
        out.writeByte(8);
        out.writeInt(getId());
        out.writeInt(getViewId());
        out.writeInt(getCliId());
        out.writeByte(getSender());
        writeExtDsts(out);
        writeExtHst(out);
        writeExtNotifList(out);
        out.writeInt(dstsFreq.size());
        for(String key : dstsFreq.keySet()){
            byte[] str = key.getBytes(StandardCharsets.UTF_8);
            out.writeInt(str.length);
            out.write(str);
            out.writeInt(dstsFreq.get(key));
        }
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
            case 7: readExtViewChange(in); break;
            case 8: readExtDestsFreq(in); break;
            case 9: readExtReady(in); break;
            case 10: readExtEnd(in); break;
        }
    }

    private void readExtMsg(ObjectInput in) throws IOException {
        setType(Type.MSG);
        setId(in.readInt());
        setViewId(in.readInt());
        setCliId(in.readInt());
        setSender(in.readShort());
        readExtDsts(in);
        readExtHst(in);
        readExtNotifList(in);
        readExtPayload(in);
    }

    private void readExtPayload(ObjectInput in) throws IOException {
        short transtype = in.readByte();
        
        switch(transtype){
            case 10: setTransaction(TransactionType.NOPAYLOAD); return;
            case 1: {
                setTransaction(TransactionType.NEW);
                int i = in.readInt();
                for(int j = 0; j < i; j++){
                    int itemid = in.readInt();
                    int qty = in.readInt();
                    items.add(new OrderItem(itemid, qty));
                }
                break;
            }
            case 2: {
                setTransaction(TransactionType.PAYMENT);
                paymentAmount = in.readDouble();
                break;
            }
            case 3: {
                setTransaction(TransactionType.STATUS);
                break;
            }
            case 4: {
                setTransaction(TransactionType.DELIVERY);
                carrierid_or_threshold = in.readInt();
                break;
            }
            case 5: {
                setTransaction(TransactionType.STOCK);
                carrierid_or_threshold = in.readInt();
                break;
            }
        }
        orderDate = new Date(in.readLong());
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
        setViewId(in.readInt());
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
        setViewId(in.readInt());
        setSender(in.readShort());
        setIdNotif(in.readInt());
        readExtDsts(in);
        readExtHst(in);
        readExtNotifList(in);
    }

    private void readExtConn(ObjectInput in) throws IOException {
        setType(Type.CONN);
        setViewId(in.readInt());
        setCliId(in.readInt());
        setSender(in.readByte());
    }

    private void readExtReady(ObjectInput in) throws IOException {
        setType(Type.READY);
        setViewId(in.readInt());
        setCliId(in.readInt());
        setSender(in.readByte());
    }

    private void readExtEnd(ObjectInput in) throws IOException {
        setType(Type.END);
        setViewId(in.readInt());
        setCliId(in.readInt());
        setSender(in.readByte());
    }

    private void readExtReply(ObjectInput in) throws IOException {
        setType(Type.REPLY);
        setViewId(in.readInt());
        setSender(in.readByte());
    }

    private void readExtGC(ObjectInput in) throws IOException {
        setType(Type.GC);
        setId(in.readInt());
        setViewId(in.readInt());
        setCliId(in.readInt());
        setSender(in.readByte());
    }

    private void readExtViewChange(ObjectInput in) throws IOException {
        setType(Type.VIEWCHANGE);
        setId(in.readInt());
        setViewId(in.readInt());
        setCliId(in.readInt());
        setSender(in.readByte());
        short size = in.readShort();
        newOverlay = new short[size];
        for(int i = 0; i < size; i++){
            newOverlay[i] = in.readShort();
        }
        readExtDsts(in);
        readExtHst(in);
        readExtNotifList(in);
    }

    private void readExtDestsFreq(ObjectInput in) throws IOException {
        setType(Type.CKFREQ);
        setId(in.readInt());
        setViewId(in.readInt());
        setCliId(in.readInt());
        setSender(in.readByte());
        readExtDsts(in);
        readExtHst(in);
        readExtNotifList(in);
        int size = in.readInt();
        for(int i = 0; i < size; i++){
            byte[] s = new byte[in.readInt()];
            in.read(s);
            String key = new String(s, StandardCharsets.UTF_8);
            dstsFreq.put(key, in.readInt());
        }
    }
}
