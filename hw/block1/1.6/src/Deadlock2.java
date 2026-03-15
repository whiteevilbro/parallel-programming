public class Deadlock2 {

    static volatile Runnable lambda = null;

    public static void main(String... args) throws InterruptedException {
        Thread A = new Thread(() -> {
            lambda.run();
        });
        Thread B = new Thread(() -> {
            try {
                A.join();
            } catch (InterruptedException ignored) {
            }
        });
        lambda = () -> {
            try {
                B.join();
            } catch (InterruptedException ignored) {
            }
        };
        A.start();
        B.start();
        A.join();
        B.join();
    }
}
