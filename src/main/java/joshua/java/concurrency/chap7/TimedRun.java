package joshua.java.concurrency.chap7;

import joshua.java.concurrency.utils.UtilityHelper;

import java.util.concurrent.*;

/**
 * Attempt at running an arbitrary Runnable for a given amount of time. problems need to address:
 * 1) what if runnable throw uncheckedException;
 *
 *
 *
 * Created by krystal on 3/1/15.
 */
public abstract class TimedRun {

    public abstract void timeRun(Runnable r, long timeout, TimeUnit unit) throws InterruptedException;
}

/**
 * Bad Attempt.
 * 1) If the runnable exits normally within the timeout. the cancelling code will interrupt the normal thread.
 * 2) Further if the runnable doesn't response to interrupt event, the timoutRun doesn't take effect.
 */
class Solution1 extends TimedRun{

    private static final ScheduledExecutorService cancelExec = Executors.newScheduledThreadPool(1);
    @Override
    public void timeRun(Runnable r, long timeout, TimeUnit unit) {
        final Thread taskThread=Thread.currentThread();
        cancelExec.schedule(new Runnable() {
            @Override
            public void run() {
                taskThread.interrupt();
            }
        },timeout,unit);
        r.run();
    }
}

/**
 * Good Attempt.
 * 1) If the runnable throws any exception, it will be kept and rethrown after timeout expires.
 * 2) If the runnable exceeds the timeout in join method, the calling thread can still return immediately.
 *
 * The only Drawback:
 * when the calling method return, you don't know it exits normally or because the join timeout.
 * so the task is not really cancelled after timeout if it can't complete within join method.
 */
class Solution2 extends TimedRun{

    private static final ScheduledExecutorService cancelExec = Executors.newScheduledThreadPool(1);

    @Override
    public void timeRun(final Runnable r, long timeout, TimeUnit unit) throws InterruptedException {

        class RethrowableTask implements Runnable{
            private volatile Throwable t;

            public void run(){
                try{
                    r.run();
                }catch (Throwable t){
                    this.t=t;
                }
            }

            void rethrow(){
                if(t!=null) {
                    UtilityHelper.launderThrowable(t);
                }
            }
        }

        final RethrowableTask task=new RethrowableTask();
        final Thread taskThread=new Thread(task);
        taskThread.start();
        cancelExec.schedule(new Runnable() {
            @Override
            public void run() {
                taskThread.interrupt();
            }
        },timeout,unit);
        /*the join will return control if timeout expires, no matter the taskThread completes or not*/
        taskThread.join(unit.toMillis(timeout));
        task.rethrow();
    }
}

/**
 * Better Attempt.
 * use get() method of Future to achieve timed run.
 * and it can cancel the task if the result is not needed anymore by cancel() method of Future.
 */
 class Solution3 extends TimedRun{
    private static final ExecutorService taskExec=Executors.newCachedThreadPool();

     @Override
     public void timeRun(Runnable r, long timeout, TimeUnit unit) throws InterruptedException {
         Future<?> task = taskExec.submit(r);
         try{
             /**Get() method:
              * for Runnable, it returns null if succeed.
              * for Callable, it returns the result.
              */
             task.get(timeout,unit);
         }catch(TimeoutException e){/*if the get  timed out*/

         }catch(ExecutionException e){/*if the task throws exception*/
            throw UtilityHelper.launderThrowable(e.getCause());
         }finally {
             /*Harmless if task already completed.*/
             task.cancel(true);//set mayInterruptIfRunning parameter as true.
             /**
              * The task execution threads created by the standard Executor implementations implement an
              interruption policy that lets tasks be cancelled using interruption, so it is safe to set mayInterruptIfRunning when
              cancelling tasks through their Futures when they are running in a standard Executor.
              You should not interrupt a poolthread directly when attempting to cancel a task, because you won't know what task is running when the interrupt
              request is delivered ‐ do this only through the task's Future.
              */
         }

     }
 }