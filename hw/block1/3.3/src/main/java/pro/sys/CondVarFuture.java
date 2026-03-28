package pro.sys;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class CondVarFuture<T> {

    final Lock lock;
    final Condition condition;
    final Callable<T> callable;

    boolean done;
    T result;
    Throwable throwable;

    CondVarFuture(Callable<T> callable) {
        this.callable = callable;
        lock = new ReentrantLock();
        condition = lock.newCondition();
        done = false;
    }

    /**
     * Waits for computation to complete, retrieves its result
     *
     * @return result of the computation
     * @throws ExecutionException if computation thrown something
     * @throws InterruptedException if current thread was interrupted
     */
    public T get() throws ExecutionException, InterruptedException {
        lock.lock();
        try {
            while (!done && throwable == null) {condition.await();}
            if (throwable != null) {throw new ExecutionException(throwable);}
            return result;
        } finally {lock.unlock();}
    }

    /**
     * Checks if computation is done. Computation is considered done if it has returned result or has thrown an exception.
     *
     * @return status of computation.
     */
    public boolean isDone() {
        lock.lock();
        try {return done;} finally {lock.unlock();}
    }


    /**
     * Runs underlying computation, sets state of Future accordingly.
     *
     * @return true if current thread should continue execution. False if current thread should terminate.
     */
    boolean run() {
        try {Thread.sleep(10);} catch (InterruptedException e) {Thread.currentThread().interrupt();}
        lock.lock();
        try {
            result = callable.call();
            done = true;
            condition.signalAll();
        } catch (Exception e) {
            throwable = e;
            done = true;
            condition.signalAll();
        } catch (Throwable t) {
            throwable = t;
            condition.signalAll();
            return false;
        } finally {
            lock.unlock();
        }
        return true;
    }
}