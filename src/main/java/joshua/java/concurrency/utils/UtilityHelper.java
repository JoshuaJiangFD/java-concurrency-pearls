package joshua.java.concurrency.utils;

/**
 * Created by krystal on 2/9/15.
 */
public  class UtilityHelper {

    /**
     * If the Throwable is an error, throw it;
     * If it is a RuntimeException return it, otherwise throw IllegalStateException.
     *
     * @param t
     * @return
     */
    public static RuntimeException launderThrowable(Throwable t){
        if(t instanceof RuntimeException)
            return (RuntimeException)t;
        else if(t instanceof Error)
            throw (Error)t;
        else
            throw new IllegalStateException("Not unchecked",t);
    }
}
