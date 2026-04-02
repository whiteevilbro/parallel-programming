package pro.sys;

import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class SingleThreadExecutorService {

    final ThreadFactory threadFactory;
    final CondVarFutureFactory condVarFutureFactory;
    final Lock workerLock = new ReentrantLock();

    Thread worker;
    LinkedBlockingDeque<CondVarFuture<?>> futureQueue;

    protected void hook() {}

    public SingleThreadExecutorService(ThreadFactory threadFactory, CondVarFutureFactory condVarFutureFactory) {
        this.threadFactory = threadFactory;
        this.condVarFutureFactory = condVarFutureFactory;
        futureQueue = new LinkedBlockingDeque<>();
    }

    /**
     * Method representing job of a worker thread.
     */
    private void workerJob() {
        CondVarFuture<?> future;
        try {
            do {
                future = futureQueue.take();
            } while (future.run());
        } catch (InterruptedException ignored) {
        } finally {
            worker = threadFactory.newThread(this::workerJob);
            hook();
            worker.start();
        }
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
        CondVarFuture<T> future = condVarFutureFactory.newCondVarFuture(task);
        futureQueue.put(future);
        workerLock.lock();
        try {
            if (worker == null) {
                worker = threadFactory.newThread(this::workerJob);
                worker.start();
            }
        } finally {
            workerLock.unlock();
        }
        return future;
    }
}
