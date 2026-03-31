package pro.sys;

import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadFactory;

public class SingleThreadExecutorService {

    final ThreadFactory threadFactory;

    Thread worker;
    LinkedBlockingDeque<CondVarFuture<?>> futureQueue;


    public SingleThreadExecutorService(ThreadFactory threadFactory) {
        this.threadFactory = threadFactory;
        futureQueue = new LinkedBlockingDeque<>();
    }

    /**
     * Method representing job of a worker thread.
     */
    private void workerJob() {
        CondVarFuture<?> future;
        do {
            try {
                future = futureQueue.take();
            } catch (InterruptedException e) {break;}
        } while (future.run());
        worker = threadFactory.newThread(this::workerJob);
        try {Thread.sleep(100);} catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        worker.start();
    }

    /**
     * Submits a value-returning task for execution.
     *
     * @param task task to submit.
     * @param <T>  return type of task.
     * @return CondVarFuture object, representing pending result of the task.
     * @throws InterruptedException if current thread was interrupted.
     */
    public <T> CondVarFuture<T> submit(Callable<T> task) throws InterruptedException {
        CondVarFuture<T> future = new CondVarFuture<>(task);
        futureQueue.put(future);
        if (worker == null || !worker.isAlive()) {
            worker = threadFactory.newThread(this::workerJob);
            worker.start();
        }
        return future;
    }
}
