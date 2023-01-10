package datastructures;

import java.io.Serializable;
import java.util.Set;

public class NotifList implements Serializable{
    short notifier;
    Set<Short> notifList;
    public NotifList(short notifier, Set<Short> notifList) {
        this.notifier = notifier;
        this.notifList = notifList;
    }
    public short getNotifier() {
        return notifier;
    }
    public Set<Short> getNotifList() {
        return notifList;
    }
    public String toString(){
        return "Notifier "+notifier+" - "+getNotifList();
    }
}
