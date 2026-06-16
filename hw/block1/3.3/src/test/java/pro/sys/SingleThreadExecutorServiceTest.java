package pro.sys;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

class BasicThreadFactory implements ThreadFactory {

    public Thread newThread(Runnable r) {
        return new Thread(r);
    }
}

class BasicTestingVarFutureFactory implements CondVarFutureFactory {

    @Override
    public <T> CondVarFuture<T> newCondVarFuture(Callable<T> task) {
        return new TestingCondVarFuture<>(task);
    }
}

class TestingSingleThreadExecutorService extends SingleThreadExecutorService {

    public TestingSingleThreadExecutorService(ThreadFactory threadFactory,
        CondVarFutureFactory condVarFutureFactory) {
        super(threadFactory, condVarFutureFactory);
    }

    @Override
    protected void hook() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

class TestingCondVarFuture<T> extends CondVarFuture<T> {

    TestingCondVarFuture(Callable<T> callable) {
        super(callable);
    }

    @Override
    protected void hook() {
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

class SingleThreadExecutorServiceTest {

    ThreadFactory threadFactory = new BasicThreadFactory();
    CondVarFutureFactory condVarFutureFactory = new BasicTestingVarFutureFactory();

    @Test
    @Timeout(2)
    void testOneTask() {
        SingleThreadExecutorService service = new TestingSingleThreadExecutorService(threadFactory,
            condVarFutureFactory);

        CondVarFuture<Integer> future;
        try {
            future = service.submit(() -> 42);
        } catch (InterruptedException ignored) {
            fail();
            return;
        }

        try {
            assertEquals(42, future.get());
        } catch (ExecutionException | InterruptedException e) {
            fail();
        }
    }

    @Test
    @Timeout(2)
    void testMultipleTasks() {
        final int N = 50;
        Random random = new Random(42);
        int[] numbers = new int[N];
        ArrayList<CondVarFuture<Integer>> futures = new ArrayList<>(N);

        SingleThreadExecutorService service = new TestingSingleThreadExecutorService(threadFactory,
            condVarFutureFactory);

        for (int i = 0; i < N; i++) {
            int a = random.nextInt();
            int b = random.nextInt();
            numbers[i] = a + b;
            try {
                futures.add(service.submit(() -> a + b));
            } catch (InterruptedException e) {fail();}
        }

        for (int i = 0; i < N; i++) {
            try {
                assertEquals(numbers[i], futures.get(i).get());
            } catch (ExecutionException | InterruptedException e) {fail(e);}
        }
    }

    @Test
    @Timeout(5)
    void testSame() {
        SingleThreadExecutorService service = new TestingSingleThreadExecutorService(threadFactory,
            condVarFutureFactory);

        CondVarFuture<Long> future1;
        CondVarFuture<Long> future2;
        try {
            future1 = service.submit(() -> Thread.currentThread().getId());
            future2 = service.submit(() -> Thread.currentThread().getId());
        } catch (InterruptedException ignored) {
            fail();
            return;
        }

        try {
            assertEquals(future1.get(), future2.get(), "Different threads");
        } catch (ExecutionException | InterruptedException e) {
            fail();
        }
    }

    @Test
    @Timeout(2)
    void testException() {
        SingleThreadExecutorService service = new TestingSingleThreadExecutorService(threadFactory,
            condVarFutureFactory);

        CondVarFuture<Integer> future;
        try {
            future = service.submit(() -> {throw new Exception("Exception!");});
        } catch (InterruptedException ignored) {
            fail();
            return;
        }

        assertThrows(ExecutionException.class, future::get);
    }

    @Test
    @Timeout(5)
    void testLongException() {
        SingleThreadExecutorService service = new TestingSingleThreadExecutorService(threadFactory,
            condVarFutureFactory);

        CondVarFuture<Integer> future1;
        CondVarFuture<Integer> future2;
        try {
            future1 = service.submit(() -> {
                Thread.sleep(1000);
                throw new Exception("Exception!");
            });
            future2 = service.submit(() -> {
                throw new Exception("Exception!");
            });
        } catch (InterruptedException ignored) {
            fail();
            return;
        }

        assertThrows(ExecutionException.class, future1::get);
        assertThrows(ExecutionException.class, future2::get);
    }

    @Test
    @Timeout(5)
    void testThrowable() {
        SingleThreadExecutorService service = new TestingSingleThreadExecutorService(threadFactory,
            condVarFutureFactory);

        CondVarFuture<Integer> future1;
        CondVarFuture<Integer> future2;
        try {
            future1 = service.submit(() -> {
                Thread.sleep(1000);
                throw new RuntimeException("Unchecked!");
            });
            future2 = service.submit(() -> 42);
        } catch (InterruptedException ignored) {
            fail();
            return;
        }

        assertThrows(ExecutionException.class, future1::get);
        try {
            assertEquals(42, future2.get());
        } catch (ExecutionException | InterruptedException e) {
            fail();
        }
    }

    @Test
    @Timeout(5)
    void testMultipleReaders() {
        SingleThreadExecutorService service = new TestingSingleThreadExecutorService(threadFactory,
            condVarFutureFactory);

        CondVarFuture<Integer> future;
        try {
            future = service.submit(() -> {
                Thread.sleep(1000);
                return 42;
            });
        } catch (InterruptedException ignored) {
            fail();
            return;
        }

        Runnable r = () -> {
            try {
                assertEquals(42, future.get());
            } catch (ExecutionException | InterruptedException e) {
                fail();
            }
        };

        int N = 5;
        ArrayList<Thread> readers = new ArrayList<>(N);
        for (int i = 0; i < N; i++) {
            readers.add(threadFactory.newThread(r));
        }
        for (Thread thread : readers) {
            thread.start();
        }

        try {
            assertEquals(42, future.get());
            for (Thread thread : readers) {
                thread.join();
            }
        } catch (ExecutionException | InterruptedException e) {
            fail();
        }
    }

    @Test
    @Timeout(5)
    void testWorkerAccess() {
        SingleThreadExecutorService service = new TestingSingleThreadExecutorService(threadFactory,
            condVarFutureFactory);

        CondVarFuture<Integer> future1, future2;
        try {
            future1 = service.submit(() -> 42);

            service.worker.interrupt();
            Thread.sleep(10);
            long id1 = service.worker.getId();
            future2 = service.submit(() -> 84);
            long id2 = service.worker.getId();

            assertEquals(id1, id2);

        } catch (InterruptedException ignored) {
            fail();
        }

    }

    @Test
    @Timeout(5)
    void testSimultaneousSubmit() {
        SingleThreadExecutorService service = new TestingSingleThreadExecutorService(threadFactory,
            condVarFutureFactory);

        CountDownLatch latch1 = new CountDownLatch(1);
        CountDownLatch latch2 = new CountDownLatch(1);
        BlockingQueue<CondVarFuture<Long>> queue = new LinkedBlockingDeque<>(2);
        Thread threadA = threadFactory.newThread(() -> {
            try {
                latch2.countDown();
                latch1.await();
                queue.put(service.submit(() -> Thread.currentThread().getId()));
            } catch (InterruptedException e) {Thread.currentThread().interrupt();}
        });
        threadA.start();

        try {
            latch2.await();
            latch1.countDown();
            queue.put(service.submit(() -> Thread.currentThread().getId()));
            threadA.join();
            assertEquals(queue.take().get(), queue.take().get());
        } catch (InterruptedException | ExecutionException e) {
            fail();
        }
    }
}