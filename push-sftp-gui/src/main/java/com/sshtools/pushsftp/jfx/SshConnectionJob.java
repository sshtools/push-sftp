package com.sshtools.pushsftp.jfx;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

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
import com.sshtools.client.SshClient.SshClientBuilder;
import com.sshtools.client.sftp.TransferCancelledException;
import com.sshtools.common.ssh.SshException;
import com.sshtools.sequins.Progress;
import com.sshtools.sequins.Progress.Level;

public abstract class SshConnectionJob<V> implements Callable<V> {
	final static ResourceBundle RESOURCES = ResourceBundle.getBundle(SshConnectionJob.class.getName());

	public static SshConnectionJobBuilder forConnection() {
		return new SshConnectionJobBuilder();
	}
	
	public final static class SshConnectionJobBuilder extends AbstractSshConnectionJobBuilder<SshConnectionJob<SshClient>, SshConnectionJobBuilder> {
		
		SshConnectionJobBuilder() {
		}

		@Override
		public SshConnectionJob<SshClient> build() {
			return new SshConnectionJob<SshClient>(this) {

				@Override
				protected void onFailed(Exception e) {
				}

				@Override
				protected SshClient onConnected(SshClient client) throws Exception {
					return client;
				}
				
			};
		}
		
	}

	public static abstract class AbstractSshConnectionJobBuilder<J extends SshConnectionJob<?>, B extends AbstractSshConnectionJobBuilder<J, B>> {
		private Optional<Target> target = Optional.empty();
		private Optional<String> agentSocket = Optional.empty();
		private Optional<Progress> progress = Optional.empty();
		private boolean verbose;
		private Optional<String> agentName = Optional.empty();
		private Optional<PassphrasePrompt> passphrasePrompt = Optional.empty();
		private Optional<PasswordPrompt> password = Optional.empty();

		@SuppressWarnings("unchecked")
		public B withPassphrasePrompt(PassphrasePrompt passphrasePrompt) {
			this.passphrasePrompt = Optional.of(passphrasePrompt);
			return (B) this;
		}

		public B withPassword(String password) {
			return withPassword(() -> password);
		}

		public B withPassword(PasswordPrompt password) {
			return withPassword(Optional.of(password));
		}

		@SuppressWarnings("unchecked")
		public B withPassword(Optional<PasswordPrompt> password) {
			this.password = password;
			return (B) this;
		}

		public B withVerbose() {
			return withVerbose(true);
		}

		@SuppressWarnings("unchecked")
		public B withVerbose(boolean verbose) {
			this.verbose = verbose;
			return (B) this;
		}

		@SuppressWarnings("unchecked")
		public B withTarget(Target target) {
			this.target = Optional.of(target);
			return (B) this;
		}

		@SuppressWarnings("unchecked")
		public B withProgress(Progress progress) {
			this.progress = Optional.of(progress);
			return (B) this;
		}

		public B withAgentSocket(String agentSocket) {
			return withAgentSocket(Optional.of(agentSocket));
		}

		@SuppressWarnings("unchecked")
		public B withAgentSocket(Optional<String> agentSocket) {
			this.agentSocket = agentSocket;
			return (B) this;
		}

		public B withAgentName(String agentName) {
			return withAgentName(Optional.of(agentName));
		}

		@SuppressWarnings("unchecked")
		public B withAgentName(Optional<String> agentName) {
			this.agentName = agentName;
			return (B) this;
		}

		public abstract J build();
	}

	protected final Target target;
	protected final Progress progress;
	protected final Optional<String> agentSocket;
	protected final String agentName;
	protected final boolean verbose;
	protected final Optional<PassphrasePrompt> passphrasePrompt;
	protected final Optional<PasswordPrompt> password;

	protected SshConnectionJob(AbstractSshConnectionJobBuilder<?, ?> builder) {
		this.target = builder.target.orElseThrow(() -> new IllegalStateException("Target must be provided.")); //$NON-NLS-1$
		this.progress = builder.progress.orElse(Progress.sink());
		this.agentSocket = builder.agentSocket;
		this.verbose = builder.verbose;
		this.agentName = builder.agentName.orElse("PSFTP"); //$NON-NLS-1$
		this.passphrasePrompt = builder.passphrasePrompt;
		this.password = builder.password;
	}

	@Override
	public V call() throws Exception {
		try {
			var client = connect();
			return onConnected(client);
		} catch (Exception e) {
			if (e.getCause() instanceof TransferCancelledException) {
				progress.error(RESOURCES.getString("cancelled"), e); //$NON-NLS-1$
			} else {
				onFailed(e);
			}
			throw e;
		} finally {
			progress.close();
		}
	}

	protected abstract void onFailed(Exception e);

	protected abstract V onConnected(SshClient client) throws Exception;

	private SshClient connect() throws IOException, SshException {

		try (var innerProgress = this.progress.newJob(RESOURCES.getString("progress.connection"), target.username(), //$NON-NLS-1$
				target.hostname(), target.port())) {
			var ssh = SshClientBuilder.create().withTarget(target.hostname(), target.port()).withUsername(target.username()).build();

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
							innerProgress.error(RESOURCES.getString("result.failedToConnectToAgent"), e); //$NON-NLS-1$
						}
					}
				} catch (AgentNotAvailableException e) {
					if (verbose)
						innerProgress.error(RESOURCES.getString("result.noAgent")); //$NON-NLS-1$
				}
			} else {
				if (verbose)
					innerProgress.message(Level.VERBOSE, RESOURCES.getString("progress.agentAuthenticationDisable")); //$NON-NLS-1$
			}

			if (!ssh.isAuthenticated()) {
				if (target.identities()) {
					if (ssh.authenticate(new IdentityFileAuthenticator(createPassphrasePrompt()),
							TimeUnit.SECONDS.toMillis(target.authenticationTimeout()))) {
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
						if (ssh.authenticate(new PrivateKeyFileAuthenticator(identity.get(), createPassphrasePrompt()),
								TimeUnit.SECONDS.toMillis(target.authenticationTimeout()))) {
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
								if (ssh.authenticate(PasswordAuthenticator.of(new PasswordPrompt() {
									@Override
									public String get() {
										return password.orElseThrow(() -> new IllegalStateException(
												RESOURCES.getString("result.passwordImpossible"))).get(); //$NON-NLS-1$
									}

									@Override
									public void completed(boolean success, String value,
											ClientAuthenticator authenticator) {
										password.orElseThrow(() -> new IllegalStateException(
												RESOURCES.getString("result.passwordImpossible"))) //$NON-NLS-1$
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
				throw new IOException(RESOURCES.getString("result.noConnection")); //$NON-NLS-1$
			}

			if (!ssh.isAuthenticated()) {
				throw new IOException(RESOURCES.getString("result.failedAuthentication")); //$NON-NLS-1$
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
				return passphrasePrompt
						.orElseThrow(() -> new IllegalStateException(RESOURCES.getString("result.noProgress"))) //$NON-NLS-1$
						.getPasshrase(keyinfo);
			}

			@Override
			public void completed(boolean success, String value, ClientAuthenticator authenticator) {
				passphrasePrompt.orElseThrow(() -> new IllegalStateException(RESOURCES.getString("result.noProgress"))) //$NON-NLS-1$
						.completed(success, value, authenticator);
			}

		};
	}

}
