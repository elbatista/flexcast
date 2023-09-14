package util;

import java.util.Arrays;

public class MsgSize {
    private long time;
    private double size;
    private short [] dst;
    public long getTime() {
        return time;
    }
    public double getSize() {
        return size;
    }
    public MsgSize(long time, double size, short[] dst) {
        this.time = time;
        this.size = size;
        this.dst = dst;
    }
    public String toString(){
        return time + ";" + size + ";" + dst.length + ";" + Arrays.toString(dst);
    }
}
