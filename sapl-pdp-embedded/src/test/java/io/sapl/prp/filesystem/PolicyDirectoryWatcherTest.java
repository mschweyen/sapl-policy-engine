package io.sapl.prp.filesystem;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;

import org.junit.Test;

import io.sapl.prp.filesystem.DirectoryWatchEventConsumer;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PolicyDirectoryWatcherTest {

	/**
	 * Run this test and create, modify or delete files under the directory
	 * target/test-classes/policies. To terminate the test, add a file called
	 * "stop.sapl" or delete the whole directory.
	 * 
	 * @throws URISyntaxException
	 * @throws InterruptedException
	 */
	// @Test
	public void watchPoliciesDirectory() throws URISyntaxException, InterruptedException {
		Path watchDir = Paths.get(getClass().getResource("policies").toURI());
		LOGGER.info("watchDir: {}", watchDir);
		final PolicyDirectoryWatcher watcher = new PolicyDirectoryWatcher(watchDir);
		final CountDownLatch cdl = new CountDownLatch(1);
		watcher.watch(new DirectoryWatchEventConsumer() {

			private boolean isCanceled;

			@Override
			@SuppressWarnings("unchecked")
			public void onEvent(WatchEvent<?> event) {
				System.out.println("policy modification event");
				WatchEvent<Path> ev = (WatchEvent<Path>) event;
				Path filename = ev.context();
				if (filename.toString().equals("stop.sapl")) {
					cancel();
				}
			}

			@Override
			public void onComplete() {
				cdl.countDown();
			}

			@Override
			public void cancel() {
				isCanceled = true;
			}

			@Override
			public boolean isCanceled() {
				return isCanceled;
			}
		});
		cdl.await();
	}

	@Test
	@SuppressWarnings("rawtypes")
	public void handleWatchKey() {
		// given
		final WatchEvent createEvent = mock(WatchEvent.class);
		when(createEvent.kind()).thenReturn(StandardWatchEventKinds.ENTRY_CREATE);
		final WatchEvent modifyEvent = mock(WatchEvent.class);
		when(modifyEvent.kind()).thenReturn(StandardWatchEventKinds.ENTRY_MODIFY);
		final WatchEvent deleteEvent = mock(WatchEvent.class);
		when(deleteEvent.kind()).thenReturn(StandardWatchEventKinds.ENTRY_DELETE);
		final WatchEvent overflowEvent = mock(WatchEvent.class);
		when(overflowEvent.kind()).thenReturn(StandardWatchEventKinds.OVERFLOW);

		final WatchKey watchKey = mock(WatchKey.class);
		when(watchKey.pollEvents()).thenReturn(Arrays.asList(createEvent, modifyEvent, deleteEvent, overflowEvent));
		when(watchKey.isValid()).thenReturn(false);

		final DirectoryWatchEventConsumer eventConsumer = spy(DirectoryWatchEventConsumer.class);

		// when
		new PolicyDirectoryWatcher(null).handleWatchKey(watchKey, eventConsumer);

		// then
		verify(eventConsumer, times(3)).onEvent(any(WatchEvent.class));
		verify(eventConsumer, times(1)).cancel();
	}
}
