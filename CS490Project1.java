import java.util.Scanner;

public class CS490Project1
{
    public static void main(String[] args)
    {
        var consoleReader = new Scanner(System.in);

        System.out.print("How many message threads should be started?  (Enter a number): ");
        int nThreads = consoleReader.nextInt();

        System.out.println("Will run " + nThreads + " threads.");

        // 3) Create nThreads threads
        Thread[] threads = new Thread[nThreads];
        for (int i = 1; i <= nThreads; i++) {
            // 4) Per-thread delay: 1s for #1, 2s for #2, ...
            int delaySeconds = i;
            var msgWriter = new MessageWriter(String.valueOf(i), delaySeconds);
            threads[i - 1] = new Thread(msgWriter, "Writer-" + i);
            threads[i - 1].start();
        }

        // Wait for all threads to finish
        for (int i = 0; i < nThreads; i++) {
            try {
                threads[i].join();
            } catch (InterruptedException exc) {
                Thread.currentThread().interrupt();
                System.out.println("Main interrupted while joining " + threads[i].getName());
                break;
            }
        }

        System.out.println("Main Program Ended");
        //No System.exit(0) needed
    }
}
