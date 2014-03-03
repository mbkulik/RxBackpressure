package rx.examples;

import java.util.ArrayList;

import rx.Observable;
import rx.schedulers.Schedulers;

public class ColdIterableSourceOverThread {

    public static void main(String[] args) {

        int SIZE = 1000000;
        ArrayList<Integer> MILLION = new ArrayList<Integer>(SIZE);
        for (int i = 0; i < SIZE; i++) {
            MILLION.add(i);
        }

        System.out.println("Starting");
        
        Observable.from(MILLION).map((i) -> {
            return "Value_" + i;
        }).observeOn(Schedulers.newThread()).toBlockingObservable().forEach((s) -> {
            System.out.println("Received: " + s);
        });
        
    }
}
