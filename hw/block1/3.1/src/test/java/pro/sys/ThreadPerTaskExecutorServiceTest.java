package pro.sys;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadFactory;
import org.junit.jupiter.api.Test;

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
    void testDifferent() {
        ThreadPerTaskExecutorService service = new ThreadPerTaskExecutorService(threadFactory);
        final int N = 50;
        ArrayList<JoinFuture<Long>> futures = new ArrayList<>(N);

        for (int i = 0; i < N; i++) {
            futures.add(service.submit(() -> Thread.currentThread().getId()));
        }

        Set<Long> ids = new HashSet<>(N * 4 / 3);
        try {

            for (int i = 0; i < N; i++) {
                Long id = futures.get(i).get();
                if (!ids.contains(id)) {
                    ids.add(id);
                } else {
                    fail();
                }
            }

        } catch (ExecutionException e) {
            fail();
        } catch (InterruptedException ignored) {
        }
    }

    @Test
    void testLogicErrorBuggyBehaviour() {
        ThreadPerTaskExecutorService service = new ThreadPerTaskExecutorService(threadFactory);
        JoinFuture<Void> really_long = service.submit(() -> {
            Thread.sleep(100);
            return null;
        });

        Thread threadA = threadFactory.newThread(() -> {
            try {
                really_long.get();
            } catch (ExecutionException | InterruptedException ignored) {
            }
        });
        threadA.start();
        threadA.interrupt();

        try {
            assertNull(really_long.get());
        } catch (ExecutionException e) {
            fail();
        } catch (InterruptedException ignored) {
        }

        try {
            Thread.sleep(1000);
        } catch (InterruptedException ignored) {
        }

        try {
            assertDoesNotThrow(really_long::get);
            assertNull(really_long.get());
        } catch (ExecutionException e) {
            fail();
        } catch (InterruptedException ignored) {
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
            } catch (InterruptedException ignored) {
            }
        }

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
            } catch (InterruptedException ignored) {
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
        } catch (InterruptedException ignored) {
        }
    }

    @Test
    void testOneException() {
        ThreadPerTaskExecutorService service = new ThreadPerTaskExecutorService(threadFactory);
        JoinFuture<Object> future = service.submit(new ExceptionCallable());

        assertThrows(ExecutionException.class, future::get);
    }

    @Test
    void testOneTask() {
        ThreadPerTaskExecutorService service = new ThreadPerTaskExecutorService(threadFactory);
        JoinFuture<Integer> future = service.submit(new SumCallable(4, 5));
        try {
            assertEquals(9, future.get());
        } catch (ExecutionException e) {
            fail(e);
        } catch (InterruptedException ignored) {
        }
    }
}