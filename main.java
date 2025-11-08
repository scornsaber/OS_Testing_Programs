import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class main {
    // Regex: PROCESS <ms> <RT|NM> <priority> <name...>
    // Allows multiple spaces; name is the rest of the line (trimmed of trailing spaces)
    private static final Pattern PROCESS_RE = Pattern.compile(
            "^\\s*PROCESS\\s+(\\d+)\\s+(RT|NM)\\s+(\\d+)\\s+(.*\\S)\\s*$");

    public static void main(String[] args) {
        final PriorityQueue queue = new PriorityQueue();

        // Start 5 worker threads
        List<Thread> workers = new ArrayList<>(5);
        for (int i = 1; i <= 5; i++) {
            Thread t = new Thread(new Worker(queue), "Worker-" + i);
            t.start();
            workers.add(t);
        }

        // Read commands from STDIN
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(System.in, StandardCharsets.UTF_8))) {

            String line;
            while ((line = br.readLine()) != null) {
                // Normalize if any stray \r remains
                line = line.replace("\r", "");

                if (line.trim().equals("SHUTDOWN")) {
                    // Stop starting new work; workers will exit once they see null from take()
                    queue.beginShutdown();
                    break;
                }

                // Parse PROCESS lines
                Matcher m = PROCESS_RE.matcher(line);
                if (m.matches()) {
                    long ms = parseLongSafe(m.group(1), "milliseconds");
                    String type = m.group(2); // "RT" or "NM"
                    int prio = parseIntSafe(m.group(3), "priority");
                    String name = m.group(4).trim();

                    // Validate ranges per spec
                    if (ms < 0) {
                        System.err.println("Ignoring: run milliseconds must be >= 0");
                        continue;
                    }
                    if (prio < 0 || prio > 9) {
                        System.err.println("Ignoring: priority must be 0..9");
                        continue;
                    }
                    if (name.isEmpty()) {
                        System.err.println("Ignoring: process name required");
                        continue;
                    }
                    if (!type.equals("RT") && !type.equals("NM")) {
                        System.err.println("Ignoring: type must be RT or NM");
                        continue;
                    }

                    try {
                        Process p = new Process(name, ms, type, prio);
                        queue.enqueue(p);
                    } catch (IllegalStateException ise) {
                        // If shutdown raced with input, ignore further enqueue attempts
                        System.err.println("Queue shutting down; ignoring new process: " + name);
                    }
                } else if (!line.trim().isEmpty()) {
                    // Non-empty but not recognized
                    System.err.println("Ignoring unrecognized line: " + line);
                }
            }
        } catch (IOException ioe) {
            System.err.println("I/O error reading input: " + ioe.getMessage());
        }

        // Join workers: allow already-running processes to finish; no new ones will start
        for (Thread t : workers) {
            boolean joined = false;
            while (!joined) {
                try {
                    t.join();
                    joined = true;
                } catch (InterruptedException ie) {
                    // Preserve interrupt and retry join
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    private static long parseLongSafe(String s, String field) {
        try { return Long.parseLong(s); }
        catch (NumberFormatException nfe) {
            System.err.println("Invalid " + field + ": " + s);
            return -1;
        }
    }

    private static int parseIntSafe(String s, String field) {
        try { return Integer.parseInt(s); }
        catch (NumberFormatException nfe) {
            System.err.println("Invalid " + field + ": " + s);
            return -1;
        }
    }

    // Worker repeatedly take highest-priority job and run it.
    // When queue.beginShutdown() is called, take() returns null and program exit.
    private static class Worker implements Runnable {
        private final PriorityQueue queue;

        Worker(PriorityQueue queue) {
            this.queue = queue;
        }

        @Override
        public void run() {
            try {
                while (true) {
                    Process p = queue.take();     // blocks until available or shutdown
                    if (p == null) {              // shutdown observed: do not start new work
                        return;
                    }
                    p.run();
                }
            } catch (InterruptedException ie) {
                // Exit on interrupt; no new work should start
                Thread.currentThread().interrupt();
            }
        }
    }
}
