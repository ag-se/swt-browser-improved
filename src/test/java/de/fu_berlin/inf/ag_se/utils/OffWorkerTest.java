package de.fu_berlin.inf.ag_se.utils;

import de.fu_berlin.inf.ag_se.utils.OffWorker.StateException;
import de.fu_berlin.inf.ag_se.widgets.browser.threading.ExecUtils;
import de.fu_berlin.inf.ag_se.widgets.browser.threading.NoCheckedExceptionCallable;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class OffWorkerTest {

	public static final class Task implements NoCheckedExceptionCallable<Long> {
		private final int i;

		public Task(int i) {
			this.i = i;
		}

		@Override
		public Long call()  {
			System.out.println("Running " + Task.class.getSimpleName() + " #"
					+ this.i + " in " + Thread.currentThread());
            try {
                Thread.sleep(2);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return System.currentTimeMillis();
		}
	}

	@Test
	public void testNonUIAsyncExecMerged() throws InterruptedException,
            ExecutionException, TimeoutException {
		int numTasks = 1500;

		final OffWorker offWorker = new OffWorker(OffWorkerTest.class, "Test");
		List<Future<Long>> futures = new ArrayList<Future<Long>>();

		for (int i = 0; i < numTasks; i++) {
			Future<Long> future = offWorker.submit(new Task(i));
			futures.add(future);

			if (i == numTasks / 2) {
				ExecUtils.nonUIAsyncExec(new Runnable() {
					@Override
					public void run() {
						offWorker.start();
					}
				});
			}
		}

		// TimePassed passed = new TimePassed(true);

		assertEquals(numTasks, futures.size());

		long lastTimestamp = -1;
		for (Future<Long> future : futures) {
			long timestamp = future.get();
			assertTrue(timestamp > lastTimestamp);
			lastTimestamp = timestamp;
		}

		System.out.println("Running task after all have finished");
		assertTrue(offWorker.submit(new Task(numTasks)).get() > lastTimestamp);

		offWorker.shutdown();

		assertTrue(offWorker.isShutdown());

		try {
			offWorker.start();
			assertTrue(false);
		} catch (StateException e) {
		}

		assertEquals(true, true);
	}

}
