package com.sshtools.pushsftp.jfx;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import com.sshtools.client.SshClient;
import com.sshtools.client.scp.ScpClientIO;
import com.sshtools.client.tasks.FileTransferProgress;
import com.sshtools.client.tasks.PushTask.PushTaskBuilder;
import com.sshtools.client.tasks.UploadFileTask.UploadFileTaskBuilder;

public final class PushJob extends SshConnectionJob<Void> {
	final static ResourceBundle RESOURCES = ResourceBundle.getBundle(PushJob.class.getName());

	@FunctionalInterface
	public interface Reporter {
		void report(PushJob job, long length, long totalSoFar, long time);
	}

	public final static class PushJobBuilder extends AbstractSshConnectionJobBuilder<PushJob, PushJobBuilder> {
		private final List<Path> files = new ArrayList<>();
		private Optional<Reporter> reporter = Optional.empty();
		private int chunks = 3;

		public static PushJobBuilder builder() {
			return new PushJobBuilder();
		}

		public PushJobBuilder withChunks(int chunks) {
			this.chunks = chunks;
			return this;
		}

		public PushJobBuilder withReporter(Reporter reporter) {
			this.reporter = Optional.of(reporter);
			return this;
		}

		public PushJobBuilder withFiles(File... files) {
			return withFiles(Arrays.asList(files));
		}

		public PushJobBuilder withFiles(List<File> files) {
			this.files.clear();
			return addFiles(files);
		}

		public PushJobBuilder withPaths(Path... files) {
			return withPaths(Arrays.asList(files));
		}

		public PushJobBuilder withPaths(List<Path> files) {
			this.files.clear();
			return addPaths(files);
		}

		public PushJobBuilder addFiles(File... files) {
			return addFiles(Arrays.asList(files));
		}

		public PushJobBuilder addFiles(List<File> files) {
			this.files.addAll(files.stream().map(f -> f.toPath()).collect(Collectors.toList()));
			return this;
		}

		public PushJobBuilder addPaths(Path... files) {
			return addPaths(Arrays.asList(files));
		}

		public PushJobBuilder addPaths(List<Path> files) {
			this.files.addAll(files);
			return this;
		}

		public PushJob build() {
			return new PushJob(this);
		}
	}

	private final List<Path> files;
	private final int chunks;
	private final Optional<Reporter> reporter;

	private PushJob(PushJobBuilder builder) {
		super(builder);
		this.files = Collections.unmodifiableList(builder.files);
		this.chunks = builder.chunks;
		this.reporter = builder.reporter;

		updateMessage(RESOURCES.getString("waiting"));
	}

	@Override
	protected Void onConnected(SshClient client) throws Exception {
		switch (target.mode()) {
		case CHUNKED:
			client.runTask(PushTaskBuilder.create()
					.withClients((idx) -> acquireClient(idx, client))
					.withChunks(chunks)
					.withVerboseOutput()
					.withRemoteFolder(target.remoteFolder())
					.withPaths(files)
					.withIntegrityVerification(target.verifyIntegrity())
					.withIgnoreIntegrity(target.ignoreIntegrity())
					.withDigest(target.hash())
					.withProgressMessages((fmt, args) -> updateMessage(MessageFormat.format(fmt, args)))
					.withProgress(fileTransferProgress(RESOURCES.getString("progress.uploading"))). //$NON-NLS-1$
					build());
			break;
		case SCP:
			var scp = new ScpClientIO(client);
			for (var file : files) {
				try (var in = Files.newInputStream(file)) {
					scp.put(in, Files.size(file), file.toString(),
							target.remoteFolder().map(r -> r.toString()).orElse(""), true, //$NON-NLS-1$
							fileTransferProgress(RESOURCES.getString("progress.uploading")));
				}
			}
			break;
		default:
			for (var file : files) {
				client.runTask(UploadFileTaskBuilder.create().withClient(client).withLocalFile(file.toFile())
						.withRemote(target.remoteFolder())
						.withProgress(fileTransferProgress(RESOURCES.getString("progress.uploading"))). //$NON-NLS-1$
						build());
			}
			break;
		}
		updateMessage(RESOURCES.getString("completed")); //$NON-NLS-1$
		return null;
	}
	
	@Override
	protected void failed() {
		var e = exceptionNow();
		updateMessage(MessageFormat.format(RESOURCES.getString("error.failedToPush"), e, files.size(), //$NON-NLS-1$
				e.getMessage() == null ? "" : e.getMessage())); //$NON-NLS-1$
	}

	@Override
	protected void cancelled() {
		updateMessage(RESOURCES.getString("cancelled")); //$NON-NLS-1$
	}

	SshClient acquireClient(int index, SshClient defaultClient) {
		if (index == 0 || target.multiplex() || target.chunks() < 2)
			return defaultClient;
		else {
			try {
				return SshConnectionJob.forConnection().
						withTarget(target).
						withVerbose(verbose).
						withAgentSocket(agentSocket).
						withPassword(password).
						withPassphrasePrompt(passphrasePrompt).
						build().
						call();
			} catch (RuntimeException e) {
				throw e;
			} catch (Exception e) {
				throw new IllegalStateException(e);
			}
		}
	}

	private boolean report(String name, long totalSoFar, long length, long started) {

		if (totalSoFar > 0) {
			var time = (System.currentTimeMillis() - started);
			updateProgress(totalSoFar, length);
			reporter.ifPresent(r -> r.report(this, length, totalSoFar, time));
		}
		return totalSoFar >= length;
	}

	private FileTransferProgress fileTransferProgress(String messagePattern) {
		var allFilesTotal = files.stream().map(f -> {
			try {
				return Files.size(f);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}).collect(Collectors.summarizingLong(Long::longValue)).getSum();
		var total = new AtomicLong();
		var started = new AtomicLong(-1);
		
		return new RateLimitedFileTransferProgress(new FileTransferProgress() {
			private long bytesSoFar;
			private String message;

			@Override
			public void started(long bytesTotal, String file) {
				this.bytesSoFar = 0;
				if(started.get() == -1) {
					started.set(System.currentTimeMillis());
					if(files.size() == 1)
						message = file;
					else
						message = MessageFormat.format(RESOURCES.getString("fileCount"), files.size());
					updateMessage(MessageFormat.format(messagePattern, message));
				}
			}

			@Override
			public boolean isCancelled() {
				return PushJob.this.isCancelled();
			}

			@Override
			public void progressed(long bytesSoFar) {
				var add = bytesSoFar - this.bytesSoFar;
				var t = total.addAndGet(add);
				this.bytesSoFar = bytesSoFar;
				report(message, t, allFilesTotal, started.get());
			}
		}, 50);
	}
}
