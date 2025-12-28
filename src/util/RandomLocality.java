package util;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/**
 * RandomLocality
 */
public class RandomLocality {
    int numnodes = 3;
    Random rand = new Random();
    public static void main(String[] args) {
        new RandomLocality();
    }
    public RandomLocality(){
        generate();
    }
    private void generate() {
        for(int i = 0; i < numnodes; i++){
            System.err.print(i + " - ");
            Set<Integer> set = new HashSet<>();
            for(int j = 0; j < 2; j++){
                int node = rand.nextInt(numnodes);
                while(node == i || set.contains(node)) node = rand.nextInt(numnodes);
                set.add(node);
                System.err.print(node+" ");
            }
            System.err.println();
        }
    }

}