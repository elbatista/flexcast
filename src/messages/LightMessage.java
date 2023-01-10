package messages;

import java.io.Serializable;
import java.util.Arrays;
import util.BaseObj;

public class LightMessage extends BaseObj implements Serializable {
    private int id = -1;
    private short [] dst;

    public LightMessage(int id){
        this.id = id;
    }

    public LightMessage(int id, short [] dst){
        this.id = id;
        setDst(dst);
    }

    public int getId() { return id; }
    
    public short[] getDst() { return dst; }

    public void setDst(short[] dst) {
        this.dst = dst;
    }

    public int getLca(){return this.dst[0];}

    public boolean isAddressedTo(short d){
        for(short i : dst) if (i == d) return true;
        return false;
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

    @Override
    public boolean equals(Object m){
        return ((LightMessage)m).getId() == getId();
    }

    @Override
    public String toString(){
        return toString(getId(), Arrays.toString(getDst()));
    }
}
