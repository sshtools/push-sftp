package com.sshtools.pushsftp.jfx;

import java.io.IOException;
import java.nio.file.Files;
import java.text.MessageFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import com.sshtools.client.SshClientContext;
import com.sshtools.common.knownhosts.HostKeyVerification;
import com.sshtools.common.ssh.SshException;
import com.sshtools.pushsftp.jfx.Target.TargetBuilder;

import javafx.concurrent.Task;

public abstract class SshConnectionJob<V> extends Task<V> implements Callable<V> {
	final static ResourceBundle RESOURCES = ResourceBundle.getBundle(SshConnectionJob.class.getName());
	final static Logger LOG = LoggerFactory.getLogger(SshConnectionJob.class);

	public static SshConnectionJobBuilder forConnection() {
		return new SshConnectionJobBuilder();
	}

	public final static class SshConnectionJobBuilder
			extends AbstractSshConnectionJobBuilder<SshConnectionJob<SshClient>, SshConnectionJobBuilder> {

		SshConnectionJobBuilder() {
		}

		@Override
		public SshConnectionJob<SshClient> build() {
			return new SshConnectionJob<SshClient>(this) {

				@Override
				protected SshClient onConnected(SshClient client) throws Exception {
					return client;
				}

			};
		}

	}

	public static abstract class AbstractSshConnectionJobBuilder<J extends SshConnectionJob<?>, B extends AbstractSshConnectionJobBuilder<J, B>> {
		private Optional<Supplier<Target>> target = Optional.empty();
		private Optional<String> agentSocket = Optional.empty();
		private boolean verbose;
		private Optional<String> agentName = Optional.empty();
		private Optional<PassphrasePrompt> passphrasePrompt = Optional.empty();
		private Optional<PasswordPrompt> password = Optional.empty();
		private Optional<HostKeyVerification> hostKeyVerification = Optional.empty();
		private Optional<Consumer<Target>> serializer = Optional.empty();
		
		@SuppressWarnings("unchecked")
		public B withSerializer(Consumer<Target> serializer) {
			this.serializer = Optional.of(serializer);
			return (B)this;
		}

		public B withHostKeyVerification(HostKeyVerification hostKeyVerification) {
			return withHostKeyVerification(Optional.of(hostKeyVerification));
		}
		
		@SuppressWarnings("unchecked")
		public B withHostKeyVerification(Optional<HostKeyVerification> hostKeyVerification) {
			this.hostKeyVerification = hostKeyVerification;;
			return (B)this;
		}

		public B withPassphrasePrompt(PassphrasePrompt passphrasePrompt) {
			return withPassphrasePrompt(Optional.of(passphrasePrompt));
		}

		@SuppressWarnings("unchecked")
		public B withPassphrasePrompt(Optional<PassphrasePrompt> passphrasePrompt) {
			this.passphrasePrompt = passphrasePrompt;
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

		public B withTarget(Target target) {
			return withTarget(() -> target);
		}

		@SuppressWarnings("unchecked")
		public B withTarget(Supplier<Target> target) {
			this.target = Optional.of(target);
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

	protected final Supplier<Target> target;
	protected final Optional<String> agentSocket;
	protected final String agentName;
	protected final boolean verbose;
	protected final Optional<Consumer<Target>> serializer;
	protected final Optional<PassphrasePrompt> passphrasePrompt;
	protected final Optional<PasswordPrompt> password;
	protected final Optional<HostKeyVerification> hostKeyVerification;
	
	private SshClient ssh;

	protected SshConnectionJob(AbstractSshConnectionJobBuilder<?, ?> builder) {
		this.target = builder.target.orElseThrow(() -> new IllegalStateException("Target must be provided.")); //$NON-NLS-1$
		this.agentSocket = builder.agentSocket;
		this.verbose = builder.verbose;
		this.agentName = builder.agentName.orElse("PSFTP"); //$NON-NLS-1$
		this.passphrasePrompt = builder.passphrasePrompt;
		this.password = builder.password;
		this.hostKeyVerification = builder.hostKeyVerification;
		this.serializer = builder.serializer;
	}

	@Override
	public final V call() throws Exception {
		var client = connect();
		return onConnected(client);
	}

	@Override
	protected final void cancelled() {
		if(ssh != null && ssh.isConnected())
			ssh.disconnect();
		onCancelled();
	}
	
	protected void onCancelled() {
		
	}

	protected abstract V onConnected(SshClient client) throws Exception;

	private SshClient connect() throws IOException, SshException {
		
		var target = this.target.get();

		updateMessage(MessageFormat.format(RESOURCES.getString("progress.connection"), target.username(), //$NON-NLS-1$
				target.hostname(), target.port()));
		
		var ctx = new SshClientContext();
		hostKeyVerification.ifPresent(hkv -> ctx.setHostKeyVerification(hkv));
		
		var bldr = SshClientBuilder.create().
				withTarget(target.hostname(), target.port()).
				withUsername(target.username()).
				withSshContext(ctx).
				withConnectTimeout(Duration.ofSeconds(120));
		
		ssh = bldr.build();

		var identity = target.identity();
		if (identity.isPresent()) {

			if(!isCancelled() && !ssh.isConnected()) {
				ssh = bldr.build();
			}

			if (!Files.exists(identity.get())) {
				if (verbose)
					updateMessage(RESOURCES.getString("progress.noIdentityFile")); //$NON-NLS-1$
			} else {
				if (ssh.authenticate(new PrivateKeyFileAuthenticator(identity.get(), createPassphrasePrompt()),
						TimeUnit.SECONDS.toMillis(target.authenticationTimeout()))) {
					//
				} else {
					if (verbose)
						updateMessage(RESOURCES.getString("progress.badIdentityFile")); //$NON-NLS-1$
				}
			}
		} else {
			if (verbose)
				updateMessage(RESOURCES.getString("progress.noSpecificIdentity")); //$NON-NLS-1$
		}

		if (!ssh.isAuthenticated()) {
			if (target.identities()) {
				if(!isCancelled() && !ssh.isConnected()) {
					ssh = bldr.build();
				}
				
				IdentityFileAuthenticator authenticator;
				if(target.preferredIdentity().isPresent()) {
					var list = new ArrayList<>(IdentityFileAuthenticator.collectIdentities(false));
					var preferredKey = target.preferredIdentity().get();
					list.remove(preferredKey);
					list.add(0, preferredKey);
					authenticator = new IdentityFileAuthenticator(list, createPassphrasePrompt());
				}
				else {
					authenticator = new IdentityFileAuthenticator(createPassphrasePrompt());
				}
				
				if (ssh.authenticate(authenticator,
						TimeUnit.SECONDS.toMillis(target.authenticationTimeout()))) {
					//
					serializer.ifPresent(s -> {
						var path = authenticator.getCurrentPath();
						if(!path.equals(target.preferredIdentity().orElse(null))) {
							s.accept(TargetBuilder.builder().
									fromTarget(target).
									withPreferredIdentity(path).
									build());
						}
					});
				}
			} else {
				if (verbose)
					updateMessage(RESOURCES.getString("progress.ignoreDefaultIdentities")); //$NON-NLS-1$
			}
		}

		if (!ssh.isAuthenticated()) {
			if(target.agent()) {

				if(!isCancelled() && !ssh.isConnected()) {
					ssh = bldr.build();
				}
				
				try {
					try (var agent = SshAgentClient.connectOpenSSHAgent(agentName,
							agentSocket.orElse(SshAgentClient.getEnvironmentSocket()))) {
						if (ssh.authenticate(new ExternalKeyAuthenticator(agent),
								TimeUnit.SECONDS.toMillis(target.authenticationTimeout()))) {
							if (verbose)
								updateMessage(RESOURCES.getString("progress.authenticatedByAgent")); //$NON-NLS-1$
						} else {
							if (verbose && agentSocket.isPresent())
								updateMessage(RESOURCES.getString("progress.notAuthenticatedByAgent")); //$NON-NLS-1$
						}
					} catch (IOException e) {
						if (agentSocket.isPresent() && verbose) {
							updateMessage(MessageFormat.format(RESOURCES.getString("error.failedToConnectToAgent"), //$NON-NLS-1$
									e.getMessage()));
						}
					}
				} catch (AgentNotAvailableException e) {
					if (verbose)
						updateMessage(RESOURCES.getString("error.noAgent")); //$NON-NLS-1$
				}
			} else {
				if (verbose)
					updateMessage(RESOURCES.getString("progress.agentAuthenticationDisable")); //$NON-NLS-1$
			}
		}

		if (!ssh.isAuthenticated()) {
			if (target.password()) {

				if(!isCancelled() && !ssh.isConnected()) {
					ssh = bldr.build();
				}
				
				if (ssh.getAuthenticationMethods().contains("password") //$NON-NLS-1$
						|| ssh.getAuthenticationMethods().contains("keyboard-interactive")) { //$NON-NLS-1$
					

					if(target.unsafePassword().isPresent()) {
						ssh.authenticate(new PasswordAuthenticator(target.unsafePassword().get()), TimeUnit.SECONDS.toMillis(target.authenticationTimeout()));
					}
					else {
					
						for (int i = 0; i < 3; i++) {
							while (ssh.isConnected() && !ssh.isAuthenticated()) {
									if (ssh.authenticate(PasswordAuthenticator.of(new PasswordPrompt() {
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
					}
				} else {
					if (verbose)
						updateMessage(RESOURCES.getString("progress.passwordNotSupported")); //$NON-NLS-1$
				}
			} else {
				if (verbose)
					updateMessage(RESOURCES.getString("progress.passwordIgnored")); //$NON-NLS-1$
			}
		}

		if (!ssh.isConnected()) {
			throw new IOException(RESOURCES.getString("error.noConnection")); //$NON-NLS-1$
		}

		if (!ssh.isAuthenticated()) {
			throw new IOException(RESOURCES.getString("error.failedAuthentication")); //$NON-NLS-1$
		}

		if (verbose)
			updateMessage(RESOURCES.getString("progress.authenticationComplete")); //$NON-NLS-1$

		return ssh;
	}

	private PassphrasePrompt createPassphrasePrompt() {
		return new PassphrasePrompt() {
			@Override
			public String getPasshrase(String keyinfo) {
				return passphrasePrompt
						.orElseThrow(() -> new IllegalStateException(RESOURCES.getString("error.noProgress"))) //$NON-NLS-1$
						.getPasshrase(keyinfo);
			}

			@Override
			public void completed(boolean success, String value, ClientAuthenticator authenticator) {
				passphrasePrompt.orElseThrow(() -> new IllegalStateException(RESOURCES.getString("error.noProgress"))) //$NON-NLS-1$
						.completed(success, value, authenticator);
			}

		};
	}

}
