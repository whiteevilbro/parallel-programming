package pro.sys;

import java.util.concurrent.ExecutionException;

public class JoinFuture<T> {
    T result;
    Thread thread;
    Exception exception = null;

    /**
     * Retrieves task completion result, waiting for it if necessary.
     *
     * @return T result of the task computation.
     * @throws ExecutionException ex if task has thrown an exception or has been interrupted. Causing exception is listed as cause of an ex exception.
     */
    public T get() throws ExecutionException, InterruptedException {
        thread.join();
        if (exception != null)
            throw new ExecutionException(exception);
        return result;
    }


    /**
     * Checks if task is done. If task has caused an exception or was interrupted, it's considered done.
     *
     * @return boolean true if task is done.
     */
    public boolean isDone() {
        return !thread.isAlive();
    }
}