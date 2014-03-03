package rx.examples;

import java.util.ArrayList;

import rx.Observable;
import rx.schedulers.Schedulers;

public class ObservableFromIterable {

    public static void main(String[] args) {

        ArrayList<Integer> MILLION = new ArrayList<Integer>(1000000);
        for (int i = 0; i < MILLION.size(); i++) {
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
