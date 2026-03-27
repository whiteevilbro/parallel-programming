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

    public T get() throws ExecutionException, InterruptedException {
        lock.lock();
        try {
            while (!done && throwable == null) {condition.await();}
            if (throwable != null) {throw new ExecutionException(throwable);}
            return result;
        } finally {lock.unlock();}
    }

    public boolean isDone() {
        lock.lock();
        try {return done;} finally {lock.unlock();}
    }

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