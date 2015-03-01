package joshua.java.concurrency.chap7;

import java.util.concurrent.BlockingQueue;

/**
 *
 * Only code that implements a thread's interruption policy may swallow an interruption request. General-purpose task
 * and library code should never swallow interruption request, instead the task code running by a pooled thread should:
 * 1) propagate the exception;
 * 2) restore the interruption status by calling static method interrupt() so that code higher up on the call stack
 *    can deal with it;
 *
 * An special case is that, for non-cancelable task should restores interruption before exit
 *
 * Created by krystal on 3/1/15.
 */
public class NoncancelableTask {
    interface Task{}

    public Task getNextTask(BlockingQueue<Task> queue){
       /*
        * retain the interrupted state if happened on queue.take() method.
        *
        */
        boolean interrupted=false;
        try{
            while(true){
                try{
                    /*responsible to Interrupted event on current thread.*/
                    return queue.take();
                }catch (InterruptedException e){
                    interrupted=true;
                    //fall through and retry.
                }
            }
        }finally {
            /*restore the interrupted state before exit
              to make getNextTask() method responsible to interrupt event without breaking the logic inside method.
              but notify the running thread or saying methods above the calling stack be aware of this interrupted event;
             */
            if(interrupted)
                Thread.currentThread().interrupt();
        }
    }
}
