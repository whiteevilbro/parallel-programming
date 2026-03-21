package pro.sys;

import java.util.concurrent.Callable;
import java.util.concurrent.ThreadFactory;

final public class ThreadPerTaskExecutorService {
    final private ThreadFactory threadFactory;

    public ThreadPerTaskExecutorService(ThreadFactory threadFactory) {
        this.threadFactory = threadFactory;
    }


    /**
     * Submit task for computation.
     *
     * @param task Callable<? extends T> representing task.
     * @param <T> task return type.
     * @return JoinFuture<T> object, representing will-be-result of the task computation.
     */
    public <T> JoinFuture<T> submit(Callable<? extends T> task) {
        JoinFuture<T> future = new JoinFuture<>();
        future.thread = threadFactory.newThread(() -> {
            try {
                future.result = task.call();
            } catch (Exception e) {
                future.exception = e;
            }
        });
        future.thread.start();
        return future;
    }
}
