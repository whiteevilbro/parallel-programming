package pro.sys;

import java.util.concurrent.Callable;

public interface CondVarFutureFactory {
    <T> CondVarFuture<T> newCondVarFuture(Callable<T> task);
}
