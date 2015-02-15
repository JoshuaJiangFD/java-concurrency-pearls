package joshua.java.concurrency.chap4;

/**
 * Created by krystal on 2/5/15.
 */


import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Monitor based Vehicle Tracker implementation.<br>
 * <p/>
 * <b>Monitor pattern</b> follows the principle of instance confinement, which encapsulates all its mutable state and guards it with the object's own intrinsic lock.<br>
 * <p/>
 * <b>Instance confinement</b>  is one of the easiest ways to build thread-safe classes by encapsulation and an appropriate locking discipline(an intrinsic lock or explicit lock)</br>
 */
@ThreadSafe
public class MonitorVehicleTracker {
    /**
     * make this field as final.
     */
    @GuardedBy("this")
    private final Map<String, MutablePoint> locations;

    public MonitorVehicleTracker(Map<String, MutablePoint> locations) {
        this.locations = locations;
    }

    public synchronized Map<String, MutablePoint> getLocations() {
        return deepCopy(locations);
    }

    public synchronized MutablePoint getLcation(String id) {
        MutablePoint loc = locations.get(id);
        return loc == null ? null : new MutablePoint(loc);
    }

    public synchronized void setLocation(String id, int x, int y) {
        MutablePoint loc = locations.get(id);
        if (loc == null) {
            throw new IllegalArgumentException("No such ID: " + id);
            loc.x = x;
            loc.y = y;
        }
    }

    /**
     * return a consistent snapshot of the inner location data.
     * called from a synchronized method, the tracker's intrinsic lock is held for the duration of what might be a long-running copy operation(so this could degrade
     * the responsiveness of the user interface when many vehicles are tracked.)
     *
     * @param m
     * @return
     */
    private static Map<String, MutablePoint> deepCopy(Map<String, MutablePoint> m) {
        Map<String, MutablePoint> result = new HashMap<String, MutablePoint>();
        for (String id : m.keySet()) {
            result.put(id, new MutablePoint(m.get(id)));
        }
        return Collections.unmodifiableMap(result);
    }
}


