package joshua.java.concurrency.chap5;

import joshua.java.concurrency.utils.UtilityHelper;

import java.util.concurrent.*;

/**
 * <b>An result Cache implementation</b><br>
 *
 * several things un-addressed:
 *
 * 1) should check the future object in cache  if running into RuntimeException while execution;
 * 2) Cache Expiration
 *    can be achieved by using subclass of FutureTask that associates an expiration time with each result and periodically scanning
 *    the cache for expired entries;
 * 3) Cache eviction
 *    remove old entries to make room for new ones so that the cache does not consume too much memory.
 *
 *
 * Created by krystal on 2/9/15.
 */
public class Memorizer<A, V> implements Computable<A, V> {

    private final ConcurrentMap<A, Future<V>> cache = new ConcurrentHashMap<A, Future<V>>();

    private final Computable<A, V> comImpl;


    public Memorizer(Computable<A, V> comImpl) {
        this.comImpl = comImpl;
    }

    @Override
    public V compute(final A arg) throws InterruptedException {
        while(true){
            Future<V> f=cache.get(arg);
            if (f == null) {
                Callable<V> eval=new Callable<V>(){

                    @Override
                    public V call() throws Exception {
                        return comImpl.compute(arg);
                    }
                };
                FutureTask<V> ft=new FutureTask<V>(eval);
                /*
                    atomic putIfAbsent method closes the window of vulnerability in which two threads might compute the same value.
                 */
                f = cache.putIfAbsent(arg, ft);
                if(f==null){
                    f=ft;
                    ft.run();
                }
            }
            try{
                return f.get();/*may throw three exceptions: CancellationException, ExecutionException, InterruptedException*/
            }catch (CancellationException e){
                /*
                    remove the Future object from cache if it detects the computation was cancelled.
                 */
                cache.remove(arg,f);
            } catch (ExecutionException e) {
            /*
                Whatever the task code may throw((Tasks described by Callable can throw checked and unchecked exception, as well errors by which any code can throw.),
                it is wrapped in an ExecutionException and rethrown from Future.get.
            */
                throw UtilityHelper.launderThrowable(e.getCause());
            }
        }
    }

}
