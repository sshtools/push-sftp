package com.sshtools.pushsftp.jfx;

import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

public final class Target {

	public final static class TargetBuilder {

		private Optional<String> username = Optional.empty();
		private Optional<String> hostname = Optional.empty();
		private int port = 22;
		private boolean identities = true;
		private boolean agent = true;
		private boolean password = true;
		private Optional<Path> identity = Optional.empty();
		private Optional<Path> remoteFolder = Optional.empty();
		private int authenticationTimeout = 120;

		public TargetBuilder withUsername(String username) {
			return withUsername(Optional.ofNullable(username));
		}

		public TargetBuilder withRemoteFolder(String remoteFolder) {
			return withRemoteFolder(remoteFolder == null || remoteFolder.equals("") ? Optional.empty()
					: Optional.of(Path.of(remoteFolder)));
		}

		public TargetBuilder withRemoteFolder(Path remoteFolder) {
			return withRemoteFolder(Optional.of(remoteFolder));
		}

		public TargetBuilder withRemoteFolder(Optional<Path> remoteFolder) {
			this.remoteFolder = remoteFolder;
			return this;
		}

		public TargetBuilder withUsername(Optional<String> username) {
			this.username = username;
			return this;
		}

		public TargetBuilder withHostname(String hostname) {
			return withHostname(Optional.ofNullable(hostname));
		}

		public TargetBuilder withHostname(Optional<String> hostname) {
			this.hostname = hostname;
			return this;
		}

		public TargetBuilder withPort(int port) {
			this.port = port;
			return this;
		}

		public TargetBuilder withoutIdentities() {
			return withIdentities(false);
		}

		public TargetBuilder withIdentities(boolean identities) {
			this.identities = identities;
			return this;
		}

		public TargetBuilder withoutAgent() {
			return withAgent(false);
		}

		public TargetBuilder withAgent(boolean agent) {
			this.agent = agent;
			return this;
		}

		public TargetBuilder withoutPassword() {
			return withPassword(false);
		}

		public TargetBuilder withPassword(boolean password) {
			this.password = password;
			return this;
		}

		public TargetBuilder withIdentity(String identity) {
			return withIdentity(identity == null || identity.equals("") ? Optional.empty() : Optional.of(Path.of(identity)));
		}

		public TargetBuilder withIdentity(Path identity) {
			return withIdentity(Optional.of(identity));
		}

		public TargetBuilder withIdentity(Optional<Path> identity) {
			this.identity = identity;
			return this;
		}

		public TargetBuilder withAuthenticationTimeout(int authenticationTimeout) {
			this.authenticationTimeout = authenticationTimeout;
			return this;
		}

		public static TargetBuilder builder() {
			return new TargetBuilder();
		}

		public Target build() {
			return new Target(this);
		}
	}

	private final String username;
	private final String hostname;
	private final int port;
	private final boolean identities;
	private final boolean agent;
	private final boolean password;
	private final Optional<Path> identity;
	private final Optional<Path> remoteFolder;
	private final int authenticationTimeout;

	private Target(TargetBuilder builder) {
		this.username = builder.username.orElse(System.getProperty("user.name"));
		this.hostname = builder.hostname.orElse("localhost");
		this.port = builder.port;
		this.identities = builder.identities;
		this.agent = builder.agent;
		this.password = builder.password;
		this.identity = builder.identity;
		this.remoteFolder = builder.remoteFolder;
		this.authenticationTimeout = builder.authenticationTimeout;
	}

	@Override
	public int hashCode() {
		return Objects.hash(hostname, port, username);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Target other = (Target) obj;
		return Objects.equals(hostname, other.hostname) && port == other.port
				&& Objects.equals(username, other.username);
	}

	public String username() {
		return username;
	}

	public Optional<Path> remoteFolder() {
		return remoteFolder;
	}

	public String hostname() {
		return hostname;
	}

	public int port() {
		return port;
	}

	public boolean identities() {
		return identities;
	}

	public boolean agent() {
		return agent;
	}

	public boolean password() {
		return password;
	}

	public Optional<Path> identity() {
		return identity;
	}

	public int authenticationTimeout() {
		return authenticationTimeout;
	}

}
