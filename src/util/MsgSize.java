package util;

import java.util.Arrays;

public class MsgSize {
    private double size;
    private short [] dst;
    public MsgSize(double size, short[] dst) {
        this.size = size;
        this.dst = dst;
    }
    public String toString(){
        return size + ";" + dst.length + ";" + Arrays.toString(dst);
    }
}
