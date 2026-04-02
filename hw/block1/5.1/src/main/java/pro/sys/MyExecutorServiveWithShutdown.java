package pro.sys;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;

class MyExecutorServiceWithShutdown {

    private final MyExecutorService service;
    State state = State.AcceptingTasks;

    final Set<Callable<?>> waiting = new HashSet<>();
    final Set<Callable<?>> executing = new HashSet<>();

    public MyExecutorServiceWithShutdown(MyExecutorService service) {
        this.service = service;
    }

    /**
     * Submits a value-returning task for execution.
     *
     * @param task task to submit.
     * @param <T>  return type of task.
     * @return MyFuture object, representing pending result of the task.
     * @throws IllegalStateException if tried to submit task after shutdown
     */
    public synchronized <T> MyFuture<T> submit(Callable<T> task) {
        if (state == State.AcceptingTasks) {
            waiting.add(task);
            return service.submit(() -> {
                synchronized (this) {
                    if (waiting.remove(task)) {
                        executing.add(task);
                    } else {
                        throw new CancellationException();
                    }
                }
                T result = task.call();
                synchronized (this) {
                    executing.remove(task);
                    tryTerminate();
                }
                return result;
            });
        }

        throw new IllegalArgumentException();
    }

    protected synchronized void tryTerminate() {
        if (state == State.UnderShutdown && executing.isEmpty()) {
            state = State.Terminated;
            notifyAll();
        }
    }

    /**
     * Initiates an orderly shutdown in which previously submitted tasks are executed, but no new
     * tasks will be accepted. Invocation has no additional effect if already shut down. This method
     * does not wait for previously submitted tasks to complete execution. Use `awaitTermination` to
     * do that.
     */
    public synchronized void shutdown() {
        if (state == State.Terminated) return;
        state = State.UnderShutdown;
        tryTerminate();
    }

    /**
     * Checks if service has been shut down.
     *
     * @return true if service was shut down.
     */
    public synchronized boolean isShutdown() {
        return state == State.UnderShutdown || state == State.Terminated;
    }

    /**
     * Checks if service was terminated, meaning it have been shut down and all tasks had finished
     * execution.
     *
     * @return true if service has been terminated.
     */
    public synchronized boolean isTerminated() {
        return state == State.Terminated;
    }

    /**
     * Forbids submission of new tasks (equivalent to `shutdown`), halts the processing of waiting
     * tasks and returns a list of the tasks that were awaiting execution. This method does not wait
     * for actively executing tasks to terminate. Any already executing task **will not** be
     * returned by this method. Use `awaitTermination` to ensure all tasks are finished.
     *
     * @return List of tasks that were awaiting execution.
     */
    public synchronized List<Callable<?>> shutdownNow() {
        List<Callable<?>> result = new ArrayList<>(waiting);
        waiting.clear();
        shutdown();
        return result;
    }

    /**
     * Blocks until all tasks have completed execution after a shutdown request.
     *
     * @return true if service had terminated.
     */
    public synchronized boolean awaitTermination() throws InterruptedException {
        while (!executing.isEmpty()) wait();
        return true;
    }

    enum State {AcceptingTasks, UnderShutdown, Terminated}
}