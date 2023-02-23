package com.sshtools.pushsftp.jfx;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.sshtools.agent.client.SshAgentClient;
import com.sshtools.agent.exceptions.AgentNotAvailableException;
import com.sshtools.client.ClientAuthenticator;
import com.sshtools.client.ExternalKeyAuthenticator;
import com.sshtools.client.IdentityFileAuthenticator;
import com.sshtools.client.PassphrasePrompt;
import com.sshtools.client.PasswordAuthenticator;
import com.sshtools.client.PasswordAuthenticator.PasswordPrompt;
import com.sshtools.client.PrivateKeyFileAuthenticator;
import com.sshtools.client.SshClient;
import com.sshtools.client.sftp.TransferCancelledException;
import com.sshtools.client.tasks.FileTransferProgress;
import com.sshtools.client.tasks.PushTask.PushTaskBuilder;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.util.FileUtils;
import com.sshtools.common.util.IOUtils;
import com.sshtools.sequins.Progress;
import com.sshtools.sequins.Progress.Level;
import com.sshtools.twoslices.Toast;
import com.sshtools.twoslices.ToastType;

public final class PushJob implements Callable<Void> {
	final static ResourceBundle RESOURCES = ResourceBundle.getBundle(PushJob.class.getName());

	public final static class PushJobBuilder {
		private Optional<Target> target = Optional.empty();
		private Optional<String> agentSocket = Optional.empty();
		private final List<Path> files = new ArrayList<>();
		private Optional<Progress> progress = Optional.empty();
		private boolean verbose;
		private Optional<String> agentName = Optional.empty();
		private Optional<PassphrasePrompt> passphrasePrompt = Optional.empty();
		private Optional<PasswordPrompt> password = Optional.empty();
		private int chunks = 3;

		public static PushJobBuilder builder() {
			return new PushJobBuilder();
		}

		public PushJobBuilder withChunks(int chunks) {
			this.chunks = chunks;
			return this;
		}

		public PushJobBuilder withPassphrasePrompt(PassphrasePrompt passphrasePrompt) {
			this.passphrasePrompt = Optional.of(passphrasePrompt);
			return this;
		}

		public PushJobBuilder withPassword(String password) {
			return withPassword(() -> password);
		}

		public PushJobBuilder withPassword(PasswordPrompt password) {
			return withPassword(Optional.of(password));
		}

		public PushJobBuilder withPassword(Optional<PasswordPrompt> password) {
			this.password = password;
			return this;
		}

		public PushJobBuilder withVerbose() {
			return withVerbose(true);
		}

		public PushJobBuilder withVerbose(boolean verbose) {
			this.verbose = verbose;
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

		public PushJobBuilder withTarget(Target target) {
			this.target = Optional.of(target);
			return this;
		}

		public PushJobBuilder withProgress(Progress progress) {
			this.progress = Optional.of(progress);
			return this;
		}

		public PushJobBuilder withAgentSocket(String agentSocket) {
			return withAgentSocket(Optional.of(agentSocket));
		}

		public PushJobBuilder withAgentSocket(Optional<String> agentSocket) {
			this.agentSocket = agentSocket;
			return this;
		}

		public PushJobBuilder withAgentName(String agentName) {
			return withAgentName(Optional.of(agentName));
		}

		public PushJobBuilder withAgentName(Optional<String> agentName) {
			this.agentName = agentName;
			return this;
		}

		public PushJob build() {
			return new PushJob(this);
		}
	}

	private final Target target;
	private final List<Path> files;
	private final Progress progress;
	private final Optional<String> agentSocket;
	private final String agentName;
	private final boolean verbose;
	private final Optional<PassphrasePrompt> passphrasePrompt;
	private final Optional<PasswordPrompt> password;
	private final int chunks;

	private PushJob(PushJobBuilder builder) {
		this.target = builder.target
				.orElseThrow(() -> new IllegalStateException(RESOURCES.getString("error.noTarget"))); //$NON-NLS-1$
		this.files = Collections.unmodifiableList(builder.files);
		this.progress = builder.progress.orElse(Progress.sink());
		this.agentSocket = builder.agentSocket;
		this.verbose = builder.verbose;
		this.agentName = builder.agentName.orElse("PSFTP"); //$NON-NLS-1$
		this.passphrasePrompt = builder.passphrasePrompt;
		this.password = builder.password;
		this.chunks = builder.chunks;

	}

	@Override
	public Void call() throws Exception {
		try {
			var client = connect();
			client.runTask(PushTaskBuilder.create().withClient(client).withChunks(chunks)
					.withRemoteFolder(target.remoteFolder()).withPaths(files)
					.withProgressMessages((fmt, args) -> progress.message(Level.NORMAL, fmt, args))
					.withProgress(fileTransferProgress(progress, RESOURCES.getString("progress.uploading"))). //$NON-NLS-1$
					build());
			progress.message(Level.VERBOSE, RESOURCES.getString("completed")); //$NON-NLS-1$
			Toast.toast(ToastType.INFO, RESOURCES.getString("toast.completed.title"), //$NON-NLS-1$ //$NON-NLS-2$
					MessageFormat.format(RESOURCES.getString("toast.completed.text"), files.size(), target.username(),
							target.hostname(), target.port(), target.remoteFolder().map(Path::toString).orElse("")));
		} catch (Exception e) {
			if (e.getCause() instanceof TransferCancelledException) {
				progress.error(RESOURCES.getString("cancelled"), e); //$NON-NLS-1$
			} else {

				progress.error(RESOURCES.getString("error.failedToPush"), e, files.size(), //$NON-NLS-1$
						e.getMessage() == null ? "" : e.getMessage()); //$NON-NLS-1$

				Toast.toast(ToastType.ERROR, RESOURCES.getString("toast.error.title"), //$NON-NLS-1$ //$NON-NLS-2$
						MessageFormat.format(RESOURCES.getString("toast.error.text"), files.size(), target.username(),
								target.hostname(), target.port(),
								target.remoteFolder().map(Path::toString).orElse("")));
			}
			throw e;
		} finally {
			progress.close();
		}
		return null;
	}

	private SshClient connect() throws IOException, SshException {

		try (var innerProgress = this.progress.newJob(RESOURCES.getString("progress.connection"), target.username(), //$NON-NLS-1$
				target.hostname(), target.port())) {
			var ssh = new SshClient(target.hostname(), target.port(), target.username());

			if (target.agent()) {
				try {
					try (var agent = SshAgentClient.connectOpenSSHAgent(agentName,
							agentSocket.orElse(SshAgentClient.getEnvironmentSocket()))) {
						if (ssh.authenticate(new ExternalKeyAuthenticator(agent),
								TimeUnit.SECONDS.toMillis(target.authenticationTimeout()))) {
							if (verbose)
								innerProgress.message(Level.VERBOSE,
										RESOURCES.getString("progress.authenticatedByAgent")); //$NON-NLS-1$
						} else {
							if (verbose && agentSocket.isPresent())
								innerProgress.message(Level.VERBOSE,
										RESOURCES.getString("progress.notAuthenticatedByAgent")); //$NON-NLS-1$
						}
					} catch (IOException e) {
						if (agentSocket.isPresent() && verbose) {
							innerProgress.error(RESOURCES.getString("error.failedToConnectToAgent"), e); //$NON-NLS-1$
						}
					}
				} catch (AgentNotAvailableException e) {
					if (verbose)
						innerProgress.error(RESOURCES.getString("error.noAgent")); //$NON-NLS-1$
				}
			} else {
				if (verbose)
					innerProgress.message(Level.VERBOSE, RESOURCES.getString("progress.agentAuthenticationDisable")); //$NON-NLS-1$
			}

			if (!ssh.isAuthenticated()) {
				if (target.identities()) {
					if (ssh.authenticate(new IdentityFileAuthenticator(createPassphrasePrompt()), TimeUnit.SECONDS.toMillis(target.authenticationTimeout()))) {
						//
					}
				} else {
					if (verbose)
						innerProgress.message(Level.VERBOSE, RESOURCES.getString("progress.ignoreDefaultIdentities")); //$NON-NLS-1$
				}
			}

			var identity = target.identity();
			if (!ssh.isAuthenticated()) {
				if (identity.isPresent()) {

					if (!Files.exists(identity.get())) {
						if (verbose)
							innerProgress.message(Level.VERBOSE, RESOURCES.getString("progress.noIdentityFile")); //$NON-NLS-1$
					} else {
						if (ssh.authenticate(new PrivateKeyFileAuthenticator(identity.get(), createPassphrasePrompt()), TimeUnit.SECONDS.toMillis(target.authenticationTimeout()))) {
							//
						} else {
							if (verbose)
								innerProgress.message(Level.VERBOSE, RESOURCES.getString("progress.badIdentityFile")); //$NON-NLS-1$
						}
					}
				} else {
					if (verbose)
						innerProgress.message(Level.VERBOSE, RESOURCES.getString("progress.noSpecificIdentity")); //$NON-NLS-1$
				}
			}

			if (!ssh.isAuthenticated()) {
				if (target.password()) {
					if (ssh.getAuthenticationMethods().contains("password") //$NON-NLS-1$
							|| ssh.getAuthenticationMethods().contains("keyboard-interactive")) { //$NON-NLS-1$
						for (int i = 0; i < 3; i++) {
							while (ssh.isConnected() && !ssh.isAuthenticated()) {
								if (ssh.authenticate(new PasswordAuthenticator(new PasswordPrompt() {
									@Override
									public String get() {
										return password.orElseThrow(() -> new IllegalStateException(
												RESOURCES.getString("error.passwordImpossible"))).get(); //$NON-NLS-1$
									}

									@Override
									public void completed(boolean success, String value,
											ClientAuthenticator authenticator) {
										password.orElseThrow(() -> new IllegalStateException(
												RESOURCES.getString("error.passwordImpossible"))) //$NON-NLS-1$
												.completed(success, value, authenticator);
									}

								}), TimeUnit.SECONDS.toMillis(target.authenticationTimeout()))) {
									break;
								}
							}
						}
					} else {
						if (verbose)
							innerProgress.message(Level.VERBOSE, RESOURCES.getString("progress.passwordNotSupported")); //$NON-NLS-1$
					}
				} else {
					if (verbose)
						innerProgress.message(Level.VERBOSE, RESOURCES.getString("progress.passwordIgnored")); //$NON-NLS-1$
				}
			}

			if (!ssh.isConnected()) {
				throw new IOException(RESOURCES.getString("error.noConnection")); //$NON-NLS-1$
			}

			if (!ssh.isAuthenticated()) {
				throw new IOException(RESOURCES.getString("error.failedAuthentication")); //$NON-NLS-1$
			}

			if (verbose)
				innerProgress.message(Level.VERBOSE, RESOURCES.getString("progress.authenticationComplete")); //$NON-NLS-1$

			return ssh;
		}
	}

	private PassphrasePrompt createPassphrasePrompt() {
		return new PassphrasePrompt() {
			@Override
			public String getPasshrase(String keyinfo) {
				return passphrasePrompt.orElseThrow(
						() -> new IllegalStateException(RESOURCES.getString("error.noProgress"))) //$NON-NLS-1$
						.getPasshrase(keyinfo);
			}

			@Override
			public void completed(boolean success, String value, ClientAuthenticator authenticator) {
				passphrasePrompt.orElseThrow(
						() -> new IllegalStateException(RESOURCES.getString("error.noProgress"))) //$NON-NLS-1$
						.completed(success, value, authenticator);
			}

		};
	}

	private boolean report(Progress progress, String name, long totalSoFar, long length, long started) {

		boolean isDone = false;
		if (totalSoFar > 0) {

			String state = RESOURCES.getString("eta"); //$NON-NLS-1$
			if (totalSoFar >= length) {
				state = RESOURCES.getString("done"); //$NON-NLS-1$
				isDone = true;
			}

			var percentage = ((double) totalSoFar / (double) length) * 100;
			var percentageStr = String.format("%.0f%%", percentage); //$NON-NLS-1$

			var humanBytes = IOUtils.toByteSize(totalSoFar);

			var time = (System.currentTimeMillis() - started);

			var megabytesPerSecond = (totalSoFar / time) / 1024D;
			var transferRate = String.format("%.1fMB/s", megabytesPerSecond); //$NON-NLS-1$

			var remaining = (length - totalSoFar);
			var perSecond = (long) (megabytesPerSecond * 1024);
			var seconds = (remaining / perSecond) / 1000l;

			var output = String.format("%s %4s %8s %10s %5d:%02d %-4s", name, percentageStr, humanBytes, transferRate, //$NON-NLS-1$
					(int) (seconds > 60 ? seconds / 60 : 0), (int) (seconds % 60), state);

			if (isDone)
				progress.message(Level.NORMAL, output, Optional.of((int) percentage));
			else
				progress.progressed(Optional.of((int) percentage), Optional.of(output));
		}
		return isDone;
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
