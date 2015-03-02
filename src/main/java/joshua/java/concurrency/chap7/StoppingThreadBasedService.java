package joshua.java.concurrency.chap7;

import net.jcip.annotations.GuardedBy;

import java.net.URL;
import java.util.*;
import java.util.concurrent.*;

/**
 * Created by krystal on 3/2/15.
 */
public class StoppingThreadBasedService {

    /**
     * Usage Scenario
     * If a method needs to process a batch of tasks and does not return until all the tasks are finished, it can simplify service
     * lifecycle management by using a private Executor whose lifetime is bounded by that method. (The invokeAll and
     * invokeAny methods can often be useful in such situations.)
     *
     * @param tasks
     * @param timeout
     * @param unit
     * @throws InterruptedException
     */
    public void oneShotExecutionService(List<Runnable> tasks,long timeout,TimeUnit unit)
            throws InterruptedException {
        ExecutorService exec= Executors.newCachedThreadPool();
        try {
            for(Runnable task:tasks){
                exec.execute(task);
            }
        }finally {
            /**
             * shutDown(): mark the executor as "Shutdown" state(total "Running", "Shutdown", "terminated")
             * All submitted tasks will be completed, but no more new tasks accepted.
             * After all tasks are completed, mark the executor as "Terminated" state.
             * Tasks submitted to an ExecutorService after it has been shut down are handled by the rejected execution handler.
             */
            exec.shutdown();
            /**
             * It is common to follow shutdown immediately by awaitTermination, creating
             *the effect of synchronously shutting down the ExecutorService.
             */
            exec.awaitTermination(timeout, unit);
        }
    }
}

class TrackingExecutor extends AbstractExecutorService {
    private final ExecutorService exec;
    private final Set<Runnable> taskCancelledAtShutDown =
            Collections.synchronizedSet(new HashSet<Runnable>());

    public TrackingExecutor(ExecutorService exec) {
        this.exec = exec;
    }

    @Override
    public void shutdown() {
        exec.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        return exec.shutdownNow();
    }

    @Override
    public boolean isShutdown() {
        return exec.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return exec.isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return exec.awaitTermination(timeout, unit);
    }

    public List<Runnable> getCancelledTasks(){
        if(!exec.isTerminated())
            throw new IllegalStateException(/*....*/);
        return new ArrayList<Runnable>(taskCancelledAtShutDown);
    }

    public void execute(final Runnable runnable) {
        exec.execute(new Runnable() {
            @Override
            public void run() {
                try{
                    runnable.run();
                }finally {
                    /**
                     * add the thread to cancelledRecords if the running thread is interrupted and the whole
                     * ExecuteService is shutdown.
                     */
                    if (isShutdown()&& Thread.currentThread().isInterrupted()) {
                        taskCancelledAtShutDown.add(runnable);
                    }
                }
            }
        });
    }
}

/**
 * An application of TrackingExecutor
 */
abstract class WebCrawler {
    private volatile TrackingExecutor exec;

    @GuardedBy("this")
    private final Set<URL> urlsToCrawl = new HashSet<URL>();

    private final ConcurrentMap<URL, Boolean> seen=new ConcurrentHashMap<URL, Boolean>();

    private static final long TIMEOUT=500;

    private static final TimeUnit UNIT=TimeUnit.MILLISECONDS;

    public WebCrawler(URL startURL) {

    }

    protected abstract List<URL> processpage(URL url);

    public synchronized void start() {
        exec = new TrackingExecutor(Executors.newCachedThreadPool());
        for (URL url : urlsToCrawl) submitCrawlTask(url);
        urlsToCrawl.clear();
    }

    public synchronized void stop() throws InterruptedException {
        try {
            saveUnCrawled(exec.shutdownNow());/*As soon as  exec marked as "ShutDown" state, all un-started tasks will be returned.*/
            if (exec.awaitTermination(TIMEOUT, UNIT))/*return true when exec reaches "Terminated" state and current thread is not interrupted.*/
                saveUnCrawled(exec.getCancelledTasks());
        } finally {
            exec = null;
        }
    }

    private void submitCrawlTask(URL u) {
        exec.execute(new CrawlTask(u));
    }

    private void saveUnCrawled(List<Runnable> uncrawled) {
        for (Runnable task : uncrawled) {
            urlsToCrawl.add(((CrawlTask) task).getPage());
        }
    }

    private class CrawlTask implements Runnable{
        private final URL url;

        public CrawlTask(URL url) {
            this.url = url;
        }

        private int count=1;

        boolean alreadyCrawled(){
            return seen.putIfAbsent(url,true)!=null;
        }

        void markUnCrawled() {
            seen.remove(url);
            System.out.printf("marking %s uncrawled.", url);
        }

        public void run(){
            for(URL link:processpage(url)){
                if(Thread.currentThread().isInterrupted())
                    return;
                submitCrawlTask(link);
            }
        }

        public URL getPage() {
            return url;
        }
    }
}