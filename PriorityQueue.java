
import java.util.ArrayDeque;
// Used double ended queue to implement priorityQueue
public class PriorityQueue {
    // Ten priority buckets (0..9)
    private final ArrayDeque<Process>[] rtBuckets = new ArrayDeque[10];
    private final ArrayDeque<Process>[] nmBuckets = new ArrayDeque[10];

    // If true, consumers must not start any new work
    private volatile boolean shutdown = false;


    // Creates the the arrays at each index to hold all of processes at that priority
    public PriorityQueue() {
        for (int i = 0; i < 10; i++) {
            rtBuckets[i] = new ArrayDeque<>();
            nmBuckets[i] = new ArrayDeque<>();
        }
    }

    /**
     * Enqueue a process into the appropriate class/priority bucket.
     * Throws if shutdown has begun.
     */
    public synchronized void enqueue(Process p) {
        if (p == null) throw new IllegalArgumentException("process is null");
        if (shutdown) throw new IllegalStateException("Queue is shutting down; no new processes accepted.");

        int pr = p.getPriority();
        if (pr < 0 || pr > 9) throw new IllegalArgumentException("priority must be 0..9");

        // Place the process into the correct queue (RT or NM) based on its type, or throw an error if invalid.
        if ("RT".equals(p.getType())) {
            rtBuckets[pr].addLast(p);
        } else if ("NM".equals(p.getType())) {
            nmBuckets[pr].addLast(p);
        } else {
            throw new IllegalArgumentException("type must be RT or NM");
        }
        notifyAll(); // wake a waiting
    }

    /**
     * Blocking take:
     *  Waits while queue is empty and shutdown has not begun.
     *  Returns the highest-priority process when available.
     *  Returns null if shutdown has begun.
     */
    public synchronized Process take() throws InterruptedException {
        while (!shutdown && isEmptyUnsafe()) {
            wait();
        }
        // If shutdown flag is set, do NOT start anything new.
        if (shutdown) return null;

        // Pull highest-priority available: RT(0..9), then NM(0..9)
        Process p = pollUnsafe();
        // Should never be null here (non-empty) but just in case
        return p;
    }

    /**
     * Begin shutdown: no consumer should start new work after this call.
     * Wakes any waiting consumers so they can observe the flag and exit cleanly.
     */
    public synchronized void beginShutdown() {
        shutdown = true;
        notifyAll();
    }

    /**
     * Whether shutdown has begun.
     */
    public boolean isShutdown() {
        return shutdown;
    }


    // private helpers

    private boolean isEmptyUnsafe() {
        for (int i = 0; i < 10; i++) {
            if (!rtBuckets[i].isEmpty()) return false;
        }
        for (int i = 0; i < 10; i++) {
            if (!nmBuckets[i].isEmpty()) return false;
        }
        return true;
    }

    private Process pollUnsafe() {
        // Strict order: all RT first (0 highest), then NM (0 highest)
        for (int i = 0; i < 10; i++) {
            if (!rtBuckets[i].isEmpty()) return rtBuckets[i].removeFirst();
        }
        for (int i = 0; i < 10; i++) {
            if (!nmBuckets[i].isEmpty()) return nmBuckets[i].removeFirst();
        }
        return null;
    }
}
