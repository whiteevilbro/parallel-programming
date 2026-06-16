package pro.sys;

import java.util.concurrent.Callable;

interface MyExecutorService {
    <T> MyFuture<T> submit(Callable<T> task);
}