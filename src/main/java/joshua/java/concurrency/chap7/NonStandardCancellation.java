package joshua.java.concurrency.chap7;

import net.jcip.annotations.GuardedBy;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.concurrent.*;

/**
 * For blocking library methods, if they are responsible to interruption, it can't be utilized to handle cancellation.
 * For non-responsible methods, we should convince threads blocked in non-interruptable activities to stop by means similar
 * to interruption, but with great awareness of why the thread is blocked.
 * 1) Synchronous socket I/O in java.io.
 * 2) Synchronous I/O in java.nio.
 * 3) Asynchronous I/O with Selector.
 * 4) Lock Acquisition.
 *   intrinsic lock: can't be responsible to interruption;
 *   explicit lock offers the lockInterruptibly() method to be responsible to interruption while waiting for a lock;
 *
 *
 * Created by krystal on 3/1/15.
 */
public class NonStandardCancellation {

    /**
     * Example One.
     * Customized thread to override interrupt() method for providing cancellation functionality.
     * The Interruption Policy of this ReadThread is:
     * 1) process all the read data;
     * 2)
     */
    class ReaderThread extends Thread {

        private static final int BUFSZ = 512;

        private final Socket socket;

        private final InputStream in;

        public ReaderThread(Socket socket) throws IOException {
            this.socket = socket;
            this.in = socket.getInputStream();
        }

        /**
         * overrode interrupt() method to provide cancellation-like functionality.
         */
        @Override
        public void interrupt() {
            try {
                socket.close();
            } catch (IOException ignored) {

            } finally {
                super.interrupt();
            }
        }


        @Override
        public void run() {
            try {
                byte[] buf = new byte[BUFSZ];
                while (true) {
                    int count = in.read(buf); /*Synchronous socket I/O method read(), which is non-responsible to interruption.*/
                    if (count < 0) {
                        break;
                    } else if (count > 0) {
                        processBuffer(buf, count);
                    }
                }
            } catch (IOException e) {
                /*if the socket is closed in interrupt() method, IOException will be caught by in.read() and the thread exit.*/
            }
        }

        public void processBuffer(byte[] buf, int count) {

        }
    }


    /**
     * Example 2.
     *  Encapsulating nonstandard cancellation in a task with newTask().
     *

     * @param <T>
     */
    interface CancellableTask <T> extends Callable<T> {
        void cancel();
        RunnableFuture<T> newTask();
    }

    public abstract class SocketUsingTask <T> implements CancellableTask<T> {
        @GuardedBy("this")
        private Socket socket;

        protected synchronized void setSocket(Socket socket) {
            this.socket = socket;
        }

        @Override
        public RunnableFuture<T> newTask() {
            return new FutureTask<T>(this) {
                @Override
                public boolean cancel(boolean mayInterruptIfRunning) {
                    try {
                        SocketUsingTask.this.cancel();/*cancel() defined in CancellableTask to close the underlying interrupt non-responsible socket.*/
                    } finally {
                        return super.cancel(mayInterruptIfRunning);/*cancel() defined in Future interface to cancel the working thread.*/
                    }
                }
            };
        }

        @Override
        public synchronized void cancel() {
            try {
                if (socket != null)
                    socket.close();
            } catch (IOException ignored) {
            }

        }
    }

    class CancellingExecutor extends ThreadPoolExecutor {

        public CancellingExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue) {
            super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
        }

        public CancellingExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory) {
            super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory);
        }

        public CancellingExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, RejectedExecutionHandler handler) {
            super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, handler);
        }

        public CancellingExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory, RejectedExecutionHandler handler) {
            super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
        }

        /**
         *Overrode newTaskFor() method to return a Future supporting cancellation.
         */
        @Override
        protected <T> RunnableFuture<T> newTaskFor(Callable<T> callable){
            /**
             * to support cancellation blocked on non-interruptable methods.
             */
            if(callable instanceof CancellableTask)
                return ((CancellableTask<T>) callable).newTask();
            /**
             * return a normal future object to support cancellation.
             */
            else
                return super.newTaskFor(callable);
        }
    }
}
