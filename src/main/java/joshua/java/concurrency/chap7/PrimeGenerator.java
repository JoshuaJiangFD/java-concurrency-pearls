package joshua.java.concurrency.chap7;

import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import static java.util.concurrent.TimeUnit.SECONDS;
import java.util.concurrent.Executors;

/**
 * Cancellation policy a task includes:
 * <p/>
 * 1) specify the "how", "when" and "what" of cancellation;
 * 2) how other code can request cancellation;
 * 3) when the task checks cancellation has been requested;
 * 4) what actions the task takes in response to a cancellation request;
 * <p/>
 * This is an example of using volatile field to hold cancellation state
 * <p/>
 * Created by krystal on 2/27/15.
 */
@ThreadSafe
public class PrimeGenerator implements Runnable {

    private static ExecutorService exec = Executors.newCachedThreadPool();

    @GuardedBy("this")
    private final List<BigInteger> primes = new ArrayList<BigInteger>();

    private volatile boolean cancelled;

    public void run() {
        BigInteger p = BigInteger.ONE;
        while (!cancelled) {
            p = p.nextProbablePrime();
            synchronized (this) {
                primes.add(p);
            }
        }
    }

    public void cancel() {
        cancelled = true;
    }

    public synchronized List<BigInteger> get(){
        return new ArrayList<BigInteger>(primes);
    }

    /**
     * call entrance of the Generator task to
     * generate a second's worth of prime numbers.
     *
     * @return
     * @throws InterruptedException
     */
    static List<BigInteger> aSecondPrimes() throws InterruptedException {
        PrimeGenerator generator=new PrimeGenerator();
        exec.execute(generator);
        try{
            SECONDS.sleep(1);
        }finally {
            generator.cancel();/*call the task to cancel*/
            /**
             * drawback:
             * if the generator exits due to unchecked runtime exception before the timeout expires.
             * it will probably go unnoticed, since the prime generator runs in a separate thread that doesn't explicitly handle exceptions.
             */
        }
        return generator.get();
    }
}
