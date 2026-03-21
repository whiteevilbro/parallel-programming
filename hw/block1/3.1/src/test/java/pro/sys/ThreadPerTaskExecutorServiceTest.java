package pro.sys;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadFactory;

import static org.junit.jupiter.api.Assertions.*;

class BasicThreadFactory implements ThreadFactory {
    public Thread newThread(Runnable r) {
        return new Thread(r);
    }
}

class SumCallable implements Callable<Integer> {
    int a;
    int b;

    public SumCallable(int a, int b) {
        this.a = a;
        this.b = b;
    }

    @Override
    public Integer call() {
        return a + b;
    }
}

class ExceptionCallable implements Callable<Object> {
    @Override
    public Object call() throws Exception {
        throw new Exception("Exceptional callable!");
    }
}

class ThreadPerTaskExecutorServiceTest {
    ThreadFactory threadFactory = new BasicThreadFactory();

    @Test
    void testOneTask() {
        ThreadPerTaskExecutorService service = new ThreadPerTaskExecutorService(threadFactory);
        JoinFuture<Integer> future = service.submit(new SumCallable(4, 5));
        try {
            assertEquals(9, future.get());
        } catch (ExecutionException e) {
            fail(e);
        }
    }

    @Test
    void testMultipleTasks() {
        final int N = 50;
        Random random = new Random(42);
        int[] numbers = new int[N];
        ArrayList<JoinFuture<Integer>> futures = new ArrayList<>(N);

        ThreadPerTaskExecutorService service = new ThreadPerTaskExecutorService(threadFactory);

        for (int i = 0; i < N; i++) {
            int a = random.nextInt();
            int b = random.nextInt();
            numbers[i] = a + b;
            futures.add(service.submit(new SumCallable(a, b)));
        }

        for (int i = 0; i < N; i++) {
            try {
                assertEquals(numbers[i], futures.get(i).get());
            } catch (ExecutionException e) {
                fail(e);
            }
        }

    }

    @Test
    void testOneException() {
        ThreadPerTaskExecutorService service = new ThreadPerTaskExecutorService(threadFactory);
        JoinFuture<Object> future = service.submit(new ExceptionCallable());

        assertThrows(ExecutionException.class, future::get);
    }

    @Test
    void testMultipleTasksWithException() {
        ThreadPerTaskExecutorService service = new ThreadPerTaskExecutorService(threadFactory);

        final int N = 50;
        Random random = new Random(42);
        int[] numbers = new int[N];
        ArrayList<JoinFuture<Object>> futures = new ArrayList<>(N);

        for (int i = 0; i < N; i++) {
            if (i != N / 2) {
                int a = random.nextInt();
                int b = random.nextInt();
                numbers[i] = a + b;
                futures.add(service.submit(new SumCallable(a, b)));
            } else {
                numbers[i] = 0;
                futures.add(service.submit(new ExceptionCallable()));
            }
        }

        for (int i = 0; i < N; i++) {
            try {
                if (i != N / 2) {
                    assertEquals(numbers[i], (Integer) futures.get(i).get());
                } else {
                    JoinFuture<Object> future = futures.get(i);
                    assertThrows(ExecutionException.class, future::get);
                }
            } catch (ExecutionException e) {
                fail(e);
            }
        }

    }

    @Test
    void testNested() {
        ThreadPerTaskExecutorService service = new ThreadPerTaskExecutorService(threadFactory);

        JoinFuture<Integer> future1 = service.submit(() -> 42);
        JoinFuture<Integer> future2 = service.submit(() -> 1984);
        JoinFuture<Integer> future = service.submit(() -> future1.get() + future2.get());

        try {
            assertEquals(42 + 1984, future.get());
        } catch (ExecutionException ex) {
            fail();
        }
    }
}