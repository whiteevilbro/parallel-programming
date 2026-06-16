package pro.sys;

import java.util.concurrent.ExecutionException;

public interface MyFuture<T> {

    public T get() throws ExecutionException, InterruptedException;

    public boolean isDone();
}
