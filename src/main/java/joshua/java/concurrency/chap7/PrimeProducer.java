package joshua.java.concurrency.chap7;

import java.math.BigInteger;
import java.util.concurrent.BlockingQueue;

/**
 * Thread Interruption is a cooperative mechanism for a thread to signal another thread that it should,
 * at its convenience stop what it's doing if it feels like it
 * <p/>
 * Using Thread Interruption for cancellation.
 * <p/>
 * A good way to think about interruption is that it doesn't actually interrupt a running thread, it just requests that the thread
 * interrupt itself at the next convenient opportunity.(cancellation points.)
 * <p/>
 * Most block methods check the "Interrupted" status of a thread and throw "InterruptedException".
 * The right way of handing "Interrupted" event of a thread is to:
 * 1)throw InterruptedException, propagate the event to the next of calling stack;
 * 2)retain the "Interrupted" status of this thread
 * <p/>
 * <p/>
 * <p/>
 * Created by krystal on 2/27/15.
 */
public class PrimeProducer extends Thread {

    private final BlockingQueue<BigInteger> queue;

    public PrimeProducer(BlockingQueue<BigInteger> queue) {
        this.queue = queue;
    }

    @Override
    public void run() {
        try {
            BigInteger p = BigInteger.ONE;
            /*check the IsInterrupted status, which can be set out of the thread to cancel the task running*/
            while (!Thread.currentThread().isInterrupted())
                /*block method which is responsible to Interrupted event occurred on the running thread.*/
                queue.put(p = p.nextProbablePrime());

        } catch (InterruptedException ex) {
            /*allow thread to exit*/
        }
    }

    public void cancel() {
        /*
           Just as tasks should have a cancellation policy, thread should have an interruption policy which specifies:
           1) how a thread interrupts request(as a request to cancel is the most correct way)
           2) what units of work are considered atomic W.R.T. interruption
           3) how quickly it reacts to interruption(depends on how often the code check the Interrupted status.)
         */
        interrupt();
    }
}

