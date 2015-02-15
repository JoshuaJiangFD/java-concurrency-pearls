package joshua.java.concurrency.chap5;

/**
 * an interface with argument typed as A and return value typed as V.
 *
 * Created by krystal on 2/9/15.
 */
public interface Computable<A,V> {

    V compute(A arg) throws InterruptedException;
}
