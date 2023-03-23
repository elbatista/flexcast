package datastructures;

import messages.LightMessage;

public class Dependency {
    boolean ack;
    boolean fromDst;
    boolean notifList;
    int sender;
    int idNotifier;
    LightMessage msgDep;
    boolean solved;

    public Dependency(boolean ack, boolean fromDst, boolean notifList, int sender, int idNotifier, LightMessage msgDep) {
        this.ack = ack;
        this.fromDst = fromDst;
        this.notifList = notifList;
        this.sender = sender;
        this.idNotifier = idNotifier;
        this.msgDep = msgDep;
        this.solved = false;
    }

    public void solve(){
        solved = true;
    }
    
    public String toString(){
        return
        "(" + 
        (ack?"ack":"") + 
        (fromDst?" fromDst":"") + 
        (notifList?" notifList":"") + 
        (sender < 0 ? "" : " sender: "+sender) + 
        (idNotifier < 0 ? "" : " idNotifier: "+idNotifier) + 
        (msgDep == null ? "" : " msgDep: "+msgDep) + 
        (solved ? " solved" : "") + 
        ")";
    }
}
