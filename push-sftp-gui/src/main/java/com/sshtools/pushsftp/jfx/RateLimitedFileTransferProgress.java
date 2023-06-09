/**
 * Copyright © 2023 JAdaptive Limited (support@jadaptive.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.sshtools.pushsftp.jfx;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.sshtools.client.tasks.FileTransferProgress;

public class RateLimitedFileTransferProgress implements FileTransferProgress {

	private final long ms;
	private final FileTransferProgress delegate;
	private final Object lock = new Object();

	private ScheduledFuture<?> task;
	private volatile long bytesSoFar;
	private ScheduledExecutorService executor;

	public RateLimitedFileTransferProgress(FileTransferProgress delegate, long ms) {
		this.delegate = delegate;
		this.ms = ms;
	}

	@Override
	public void completed()  {
		synchronized(lock) {
			scheduleOne(true);
			finishTask();
			executor.shutdown();
		}
	}

	@Override
	public boolean isCancelled() {
		return delegate.isCancelled();
	}


	@Override
	public void started(long bytesTotal, String file) {
		finishTask();
		executor = Executors.newScheduledThreadPool(1);
		delegate.started(bytesTotal, file);
	}

	@Override
	public void progressed(long bytesSoFar) {
		synchronized (lock) {
			this.bytesSoFar = bytesSoFar;
			scheduleOne(false);
		}

	}

	public void scheduleOne(boolean complete) {
		if (task == null || task.isDone()) {
			task = executor.schedule(() -> {
				if(complete)
					delegate.completed();
				else
					delegate.progressed(RateLimitedFileTransferProgress.this.bytesSoFar);
			}, ms, TimeUnit.MILLISECONDS);
		}
	}

	private void finishTask() {
		synchronized(lock) {
			if (task != null) {
				try {
					task.get();
				} catch (InterruptedException | ExecutionException e) {
				} finally {
					task = null;
				}
			}
		}
	}

}
