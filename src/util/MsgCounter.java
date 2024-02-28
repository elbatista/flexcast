import java.util.Arrays;
import java.util.List;

public class MsgCounter {
    int [] topo;

    public MsgCounter(int[] t){
        this.topo = t;
    }

    boolean isDest(int n, int[] dst){
        return Arrays.stream(dst).anyMatch(d -> d == n);
    }

    int posD(int d, int[] dst){
        int pos = 1;
        for(int dd : dst){
            if(d == dd) break;
            pos++;
        }
        return pos;
    }

    int posI(int n, int[] dst){
        int pos = 1;
        for(int ii : topo){
            // skip nodes before the lca of dst
            if(ii < dst[0]) continue;
            // skip dests
            if(isDest(ii, dst)) continue;
            
            if(ii == n) break;
            pos++;
        }
        return pos;
    }

    int countPrevDests(int n, int[] dst){
        int count = 0;
        for(int ii : topo){
            // skip nodes before the lca of dst
            if(n < dst[0]) continue;
            if(ii == n) break;
            if(isDest(ii, dst))
                count++;
        }
        return count;
    }

    int multicastCost(int n, int[] dst){
        
        // lca: one msg only (data)
        if(n == dst[0]) { System.out.println(n+" is lca: return 1"); return 1;}

        // intermediary node
        if(!isDest(n, dst)){

            System.out.println(n + " is inter");
            System.out.println(n + " posI: " + posI(n, dst));
            System.out.println(n + " notificações de intermediarios anteriores: " + ((Math.pow(2, (posI(n, dst)-1))) - 1));
            System.out.println(n + " notificações de dests anteriores: " + countPrevDests(n, dst));
            System.out.println(n + " return = " + ((int)((Math.pow(2, (posI(n, dst)-1))) - 1)  +   countPrevDests(n, dst) ));

            //      notificações de intermediarios anteriores       notifs de dests anteriores 
            return ((int)((Math.pow(2, (posI(n, dst)-1))) - 1)  +   countPrevDests(n, dst) );
        }
        // dst node:
        else {

            System.out.println(n + " is dest");
            System.out.println(n + " posD: " + posD(n, dst));
            //somatorio [ ( de k: lca+1 até n-1):  se k=dst: 0       senao:  2#i(k) - 1 ]          
            //acks de intermediários
            int sumAcksInter = 0;
            for(int i : topo){
                // skip nodes before the lca (included) of dst
                if(i <= dst[0]) continue;
                // range up to n-1
                if( i == n) break;

                // intermediaries only
                if(!isDest(i, dst)){
                    sumAcksInter += (int)((Math.pow(2, (posI(i, dst)-1))) );//- 1 );
                }
            }

            System.out.println(n + " posD(n, dst)-2 = " + (posD(n, dst)-2) + "; sumAcksInter = "+sumAcksInter);
            System.out.println(n + " return = " + (1 + (posD(n, dst)-2) + sumAcksInter));

            return 1 +          // one data msg
            (posD(n, dst)-2) +  // acks from ancestors dests
            sumAcksInter;
        }
    }

    public void exec(int[] dst){
        int cost = 0;
        // Soma, para todo nodo n desde lca(dst,t) até último(dst,d)
        for(int n : topo){
            
            // skip nodes before lca
            if(n < dst[0]) continue;

            // skip nodes after the last dest
            if(n > dst[dst.length-1]) break;

            System.out.println("Calculando para " + n);

            cost += multicastCost(n, dst);
        }
        System.out.println("-----------");
        System.out.println("Cost: " + cost);
        System.out.println("----------");
    }
    

    public static void main(String[] args) {
        if(args.length < 2) {
            System.out.println("Usage: MsgCounter \"<topology>\"  \"<dst>\"  ");
            return;
        }

        String [] stopo = args[0].split(",");
        String [] sdst = args[1].split(",");
        System.out.println("Topology: " + Arrays.toString(stopo));
        System.out.println("Dst: " + Arrays.toString(sdst));
        
        int[] t = Arrays.stream(stopo).mapToInt(Integer::parseInt).toArray();
        int[] dst = Arrays.stream(sdst).mapToInt(Integer::parseInt).toArray();

        new MsgCounter(t).exec(dst);
    }
}
