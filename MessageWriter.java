import java.util.concurrent.atomic.AtomicInteger;

public class MessageWriter implements Runnable
{
    // 5) Shared counter across all threads
    private static final AtomicInteger sharedCounter = new AtomicInteger(0);

    private final int delaySeconds;   // seconds (not milliseconds)
    private final String myName;

    public MessageWriter(String name, int delaySeconds)
    {
        this.myName = name;
        this.delaySeconds = delaySeconds;
    }

    @Override
    public void run()
    {
        for (int i = 0; i < 10; i++)
        {
            try
            {
                Thread.sleep(delaySeconds * 1000L);
            }
            catch (InterruptedException exc)
            {
                Thread.currentThread().interrupt();
                System.out.println("Thread " + myName + " interrupted; exiting.");
                break;
            }

            int current = sharedCounter.incrementAndGet();
            System.out.println("Message #" + current + " - " + myName +
                               " (delay=" + delaySeconds + "s)");
        }
    }
}
