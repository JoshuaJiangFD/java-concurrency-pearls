package joshua.java.concurrency.chap4;

import net.jcip.annotations.NotThreadSafe;

/**
 * Created by krystal on 2/8/15.
 */
@NotThreadSafe
public class MutablePoint {

    public int x;
    public int y;


    public MutablePoint() {
        x=0;
        y=0;
    }

    public MutablePoint(MutablePoint p) {
        x=p.x;
        y=p.y;
    }
}
