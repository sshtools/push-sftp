package com.sshtools.pushsftp.jfx;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import com.sshtools.client.SshClient;
import com.sshtools.client.scp.ScpClientIO;
import com.sshtools.client.tasks.FileTransferProgress;
import com.sshtools.client.tasks.PushTask.PushTaskBuilder;
import com.sshtools.client.tasks.UploadFileTask.UploadFileTaskBuilder;
import com.sshtools.common.util.FileUtils;
import com.sshtools.common.util.IOUtils;
import com.sshtools.sequins.Progress;
import com.sshtools.sequins.Progress.Level;
import com.sshtools.twoslices.Toast;
import com.sshtools.twoslices.ToastType;

public final class PushJob extends SshConnectionJob<Void> {
	final static ResourceBundle RESOURCES = ResourceBundle.getBundle(PushJob.class.getName());

	@FunctionalInterface
	public interface Reporter {
		void report(String name, double percentage, long length, long totalSoFar, long time);
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

	}

	@Override
	protected void onFailed(Exception e) {

		progress.error(RESOURCES.getString("result.failedToPush"), e, files.size(), //$NON-NLS-1$
				e.getMessage() == null ? "" : e.getMessage()); //$NON-NLS-1$

		Toast.toast(ToastType.ERROR, RESOURCES.getString("toast.error.title"), //$NON-NLS-1$ //$NON-NLS-2$
				MessageFormat.format(RESOURCES.getString("toast.error.text"), files.size(), target.username(),
						target.hostname(), target.port(), target.remoteFolder().map(Path::toString).orElse("")));

	}

	@Override
	protected Void onConnected(SshClient client) throws Exception {
		switch (target.mode()) {
		case CHUNKED:
		case CHUNKED_SFTP:
			client.runTask(PushTaskBuilder.create().withClient(client).withChunks(chunks).withVerboseOutput()
					.withRemoteFolder(target.remoteFolder()).withPaths(files)
					.withIntegrityVerification(target.verifyIntegrity()).withIgnoreIntegrity(target.ignoreIntegrity())
					.withIgnoreCopyDataExtension(!target.copyDataExtension()).withPreAllocation(target.preAllocate())
					.withDigest(target.hash()).withSFTPForcing(target.mode() == Mode.CHUNKED_SFTP)
					.withProgressMessages((fmt, args) -> progress.message(Level.NORMAL, fmt, args))
					.withProgress(fileTransferProgress(progress, RESOURCES.getString("progress.uploading"))). //$NON-NLS-1$
					build());
			break;
		case SCP:
			var scp = new ScpClientIO(client);
			for (var file : files) {
				try (var in = Files.newInputStream(file)) {
					scp.put(in, Files.size(file), file.toString(),
							target.remoteFolder().map(r -> r.toString()).orElse(""), true, //$NON-NLS-1$
							fileTransferProgress(progress, RESOURCES.getString("progress.uploading")));
				}
			}
			break;
		default:
			for (var file : files) {
				client.runTask(UploadFileTaskBuilder.create().withClient(client).withLocalFile(file.toFile())
						.withRemote(target.remoteFolder())
						.withProgress(fileTransferProgress(progress, RESOURCES.getString("progress.uploading"))). //$NON-NLS-1$
						build());
			}
			break;
		}
		progress.message(Level.VERBOSE, RESOURCES.getString("completed")); //$NON-NLS-1$
		Toast.toast(ToastType.INFO, RESOURCES.getString("toast.completed.title"), //$NON-NLS-1$ //$NON-NLS-2$
				MessageFormat.format(RESOURCES.getString("toast.completed.text"), files.size(), target.username(),
						target.hostname(), target.port(), target.remoteFolder().map(Path::toString).orElse("")));
		return null;
	}

	private boolean report(Progress progress, String name, long totalSoFar, long length, long started) {

		if (totalSoFar > 0) {
			var percentage = ((double) totalSoFar / (double) length) * 100;
			var time = (System.currentTimeMillis() - started);

			reporter.orElse(new ProgressReporter(progress)).report(name, percentage, length, totalSoFar, time);
		}
		return totalSoFar >= length;
	}

	public static class ProgressReporter implements Reporter {

		private Progress progress;

		public ProgressReporter(Progress progress) {
			this.progress = progress;
		}

		@Override
		public void report(String name, double percentage, long length, long totalSoFar, long time) {
			var state = RESOURCES.getString("eta"); //$NON-NLS-1$
			if (totalSoFar >= length) {
				state = RESOURCES.getString("done"); //$NON-NLS-1$
			}
			var percentageStr = String.format("%.0f%%", percentage); //$NON-NLS-1$
			var humanBytes = IOUtils.toByteSize(totalSoFar);

			var megabytesPerSecond = (totalSoFar / time) / 1024D;
			var transferRate = String.format("%.1fMB/s", megabytesPerSecond); //$NON-NLS-1$
			var perSecond = (long) (megabytesPerSecond * 1024);
			var remaining = (length - totalSoFar);
			var seconds = (remaining / perSecond) / 1000l;

			var output = String.format("%s %4s %8s %10s %5d:%02d %-4s", name, percentageStr, humanBytes, transferRate, //$NON-NLS-1$
					(int) (seconds > 60 ? seconds / 60 : 0), (int) (seconds % 60), state);
			progress.progressed(Optional.of((int) percentage), Optional.of(output));

		}

	}

	private FileTransferProgress fileTransferProgress(Progress progress, String messagePattern) {
		return new FileTransferProgress() {
			private long bytesTotal;
			private long started;
			private String file;

			@Override
			public void started(long bytesTotal, String file) {
				this.started = System.currentTimeMillis();
				this.bytesTotal = bytesTotal;
				this.file = FileUtils.getFilename(file);
				progress.message(Level.NORMAL, messagePattern, this.file);
			}

			@Override
			public boolean isCancelled() {
				return progress.isCancelled();
			}

			@Override
			public void progressed(long bytesSoFar) {
				report(progress, file, bytesSoFar, bytesTotal, started);
			}
		};
	}
}
