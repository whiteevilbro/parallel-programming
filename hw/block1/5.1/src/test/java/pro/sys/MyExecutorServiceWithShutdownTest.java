package pro.sys;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

// Uses concurrent.* classes, but for testing purposes. Hope that's ok, because 3.1 implementation
// isn't really reliable and suitable for testing purposes.
class MyFutureImplemetation<T> implements MyFuture<T> {

    Future<T> future;

    MyFutureImplemetation(Future<T> future) {
        this.future = future;
    }

    @Override
    public T get() throws ExecutionException, InterruptedException {
        return future.get();
    }

    @Override
    public boolean isDone() {
        return future.isDone();
    }
}

class MyExecutorServiceImplementation implements MyExecutorService {

    final ExecutorService service = Executors.newFixedThreadPool(2);

    @Override
    public <T> MyFuture<T> submit(Callable<T> task) {
        return new MyFutureImplemetation<>(service.submit(task));
    }
}

class MyExecutorServiceWithShutdownTest {

    // general correctness tests
    @Test
    @Timeout(2)
    void testMultipleTasks() {
        final int N = 50;
        Random random = new Random(42);
        int[] numbers = new int[N];
        ArrayList<MyFuture<Integer>> futures = new ArrayList<>(N);

        MyExecutorServiceWithShutdown service = new MyExecutorServiceWithShutdown(
            new MyExecutorServiceImplementation());

        for (int i = 0; i < N; i++) {
            int a = random.nextInt();
            int b = random.nextInt();
            numbers[i] = a + b;
            futures.add(service.submit(() -> a + b));
        }

        for (int i = 0; i < N; i++) {
            try {
                assertEquals(numbers[i], futures.get(i).get());
            } catch (ExecutionException | InterruptedException e) {fail(e);}
        }
    }

    @Test
    @Timeout(5)
    void testLongException() {
        MyExecutorServiceWithShutdown service = new MyExecutorServiceWithShutdown(
            new MyExecutorServiceImplementation());

        MyFuture<Integer> future1;
        MyFuture<Integer> future2;
        future1 = service.submit(() -> {
            Thread.sleep(1000);
            throw new Exception("Exception!");
        });
        future2 = service.submit(() -> {
            throw new Exception("Exception!");
        });

        assertThrows(ExecutionException.class, future1::get);
        assertThrows(ExecutionException.class, future2::get);
    }

    // specific tests
    @Test
    @Timeout(5)
    void testShutdown() {
        MyExecutorServiceWithShutdown service = new MyExecutorServiceWithShutdown(
            new MyExecutorServiceImplementation());

        Callable<Integer> payload1 = () -> {
            Thread.sleep(1000);
            return 42;
        };
        Callable<Integer> payload2 = () -> {
            Thread.sleep(1000);
            return 84;
        };
        Callable<Integer> payload3 = () -> {
            Thread.sleep(1000);
            return 87;
        };
        MyFuture<Integer> future1 = service.submit(payload1);
        MyFuture<Integer> future2 = service.submit(payload2);
        try {Thread.sleep(10);} catch (InterruptedException e) {fail();}
        service.shutdown();
        assertThrows(IllegalArgumentException.class, () -> service.submit(payload3));
        try {
            assertEquals(42, future1.get());
            assertEquals(84, future2.get());
        } catch (InterruptedException | ExecutionException e) {
            fail();
        }
    }

    @Test
    @Timeout(5)
    void testShutdownNow() {
        MyExecutorServiceWithShutdown service = new MyExecutorServiceWithShutdown(
            new MyExecutorServiceImplementation());

        MyFuture<Integer> future1 = service.submit(() -> {
            Thread.sleep(1000);
            return 42;
        });
        MyFuture<Integer> future2 = service.submit(() -> {
            Thread.sleep(1000);
            return 84;
        });
        Callable<Integer> c = () -> {
            Thread.sleep(1000);
            return 19;
        };
        MyFuture<Integer> future3 = service.submit(c);

        List<Callable<?>> remaining = service.shutdownNow();
        assertTrue(remaining.size() == 1 && remaining.contains(c));
        try {
            assertEquals(42, future1.get());
            assertEquals(84, future2.get());
            assertThrows(ExecutionException.class, future3::get);
        } catch (InterruptedException | ExecutionException e) {
            fail();
        }
    }

    @Test
    @Timeout(5)
    void testTermination() {
        MyExecutorServiceWithShutdown service = new MyExecutorServiceWithShutdown(
            new MyExecutorServiceImplementation());

        MyFuture<Integer> future = service.submit(() -> {
            Thread.sleep(1000);
            return 42;
        });
        assertFalse(service.isShutdown());
        assertFalse(service.isTerminated());
        service.shutdown();
        assertTrue(service.isShutdown());
        try {
            assertTrue(service.awaitTermination());
        } catch (InterruptedException e) {
            fail();
        }
        assertTrue(service.isTerminated());
    }
}