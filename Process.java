// Process.java
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

public class Process implements Runnable {
    // For safely assgining IDs
    private static final AtomicInteger NEXT_ID = new AtomicInteger(1);
    private static final DateTimeFormatter TS =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");

    /*• An integer process ID.
    • A string name.
    • A number of milliseconds that the process should run.
    • A process class (real-time or normal).
    • A process priority (0 to 9)*/

    private final int processId;
    private final String name;
    private final long runMillis;    
    private final String type;       
    private final int priority;      

    public Process(String name, long runMillis, String type, int priority) {
        if (runMillis < 0) throw new IllegalArgumentException("runMillis must be >= 0");
        if (priority < 0 || priority > 9) throw new IllegalArgumentException("priority must be 0..9");
        if (name == null || name.isEmpty()) throw new IllegalArgumentException("name required");
        if (type == null || (!type.equals("RT") && !type.equals("NM")))
            throw new IllegalArgumentException("type must be RT or NM");

        this.processId = NEXT_ID.getAndIncrement();
        this.name = name;
        this.runMillis = runMillis;
        this.type = type;
        this.priority = priority;
    }


    /*• Print a line that has this form:
o BEGIN\t{Current Time}\t{Process ID}\t{Process Name}
o The current time must include milliseconds.
o \t is the tab character
• Sleep for the number of milliseconds that the process was specified to run.
• Print a line that has this form:
o END\t{Current Time}\t{Process ID}\t{Process Name}
• Exit */

    @Override
    public void run() {
        String nowBegin = now();
        System.out.println("BEGIN\t" + nowBegin + "\t" + processId + "\t" + name);

        try {
            Thread.sleep(runMillis);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }

        String nowEnd = now();
        System.out.println("END\t" + nowEnd + "\t" + processId + "\t" + name);
    }

    private static String now() {
        return ZonedDateTime.now().format(TS);
    }

    // Getters. Not all ended up being 
    public int getProcessId() { return processId; }
    public String getName() { return name; }
    public long getRunMillis() { return runMillis; }
    public String getType() { return type; }
    public int getPriority() { return priority; }

}
