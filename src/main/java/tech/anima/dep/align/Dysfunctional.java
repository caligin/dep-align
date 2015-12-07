package tech.anima.dep.align;

import java.util.Iterator;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

public class Dysfunctional {

    public static <T> Iterable<T> oneTimeIterable(Iterator<T> it) {
        return (Iterable) () -> it;
    }

    public static <T1, T2, R> Function<T1, R> curryMethod(BiFunction<T1, T2, R> unaryMethod, T2 argument) {
        return (self) -> unaryMethod.apply(self, argument);
    }
    
    public static <T> Predicate<T> not(Predicate<T> predicate) {
        return predicate.negate();
    }

}
