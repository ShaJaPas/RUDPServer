package main.tools.chance;

import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.ThreadLocalRandom;

public class ChanceTaker {
    public static <T extends Chance> T getElementFromArray(T[] arr) throws Exception {
        if(arr.length > 0){
            NavigableMap<Double, T> weighedMap = new TreeMap<>();
            double totalWeight = 0;
            for (T t : arr) {
                totalWeight += t.chance;
                weighedMap.put(totalWeight, t);
            }
            return weighedMap.higherEntry(ThreadLocalRandom.current().nextDouble(0, totalWeight)).getValue();
        } else throw new Exception("Array size must be greater then 0");
    }
}
