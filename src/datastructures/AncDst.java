package datastructures;

import messages.LightMessagesList.Item;

public class AncDst {
    private Item lastInPfx; 
    // private Item [] lastInNotif;
    public AncDst(short lastInNotifSize){
        // lastInNotif = new Item[lastInNotifSize];
    }
    public Item getLastInPfx() {
        return lastInPfx;
    }
    public void setLastInPfx(Item lastInPfx) {
        this.lastInPfx = lastInPfx;
    }
    // public Item [] getLastInNotif() {
    //     return lastInNotif;
    // }
}
