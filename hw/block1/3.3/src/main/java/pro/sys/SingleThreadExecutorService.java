package pro.sys;

import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadFactory;

public class SingleThreadExecutorService {

    final ThreadFactory threadFactory;

    Thread worker;
    LinkedBlockingDeque<CondVarFuture<?>> futureQueue;

    private void workerJob() {
        CondVarFuture<?> future;
        do {
            try {
                future = futureQueue.take();
            } catch (InterruptedException e) {break;}
        } while (future.run());
        worker = threadFactory.newThread(this::workerJob);
        worker.start();
    }

    public SingleThreadExecutorService(ThreadFactory threadFactory) {
        this.threadFactory = threadFactory;
        futureQueue = new LinkedBlockingDeque<>();
    }

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
