public class Deadlock1 {
    public static void main(String[] args) throws InterruptedException {
        Thread.currentThread().join();
    }
}