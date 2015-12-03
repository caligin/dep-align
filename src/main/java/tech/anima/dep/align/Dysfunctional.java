package tech.anima.dep.align;

import java.util.Iterator;

public class Dysfunctional {

    public static <T> Iterable<T> oneTimeIterable(Iterator<T> it) {
        return (Iterable) () -> it;
    }

}
