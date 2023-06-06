package com.sshtools.pushsftp.jfx;

import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

import com.sshtools.client.sftp.RemoteHash;

public final class Target {

	public final static class TargetBuilder {

		private Optional<String> displayName = Optional.empty();
		private Optional<String> username = Optional.empty();
		private Optional<String> hostname = Optional.empty();
		private int port = 22;
		private boolean identities = true;
		private boolean agent = true;
		private boolean password = true;
		private Optional<Path> identity = Optional.empty();
		private Optional<Path> remoteFolder = Optional.empty();
		private int authenticationTimeout = 120;
		private Optional<Mode> mode = Optional.empty();
		private Optional<Integer> chunks = Optional.empty();
		private boolean verifyIntegrity;
		private boolean ignoreIntegrity;
		private boolean multiplex = false;
		private Optional<RemoteHash> hash = Optional.empty();

		public TargetBuilder withVerifyIntegrity(boolean verifyIntegrity) {
			this.verifyIntegrity = verifyIntegrity;
			return this;
		}

		public TargetBuilder withVerifiedIntegrity() {
			return withVerifyIntegrity(true);
		}

		public TargetBuilder withIgnoredIntegrity() {
			return withIgnoreIntegrity(true);
		}

		public TargetBuilder withIgnoreIntegrity(boolean ignoreIntegrity) {
			this.ignoreIntegrity = ignoreIntegrity;
			return this;
		}

		public TargetBuilder withMultiplex() {
			return withMultiplex(true);
		}

		public TargetBuilder withMultiplex(boolean multiplex) {
			this.multiplex= multiplex;
			return this;
		}


		public TargetBuilder withHash(RemoteHash hash) {
			return withHash(Optional.of(hash));
		}

		public TargetBuilder withHash(Optional<RemoteHash> hash) {
			this.hash = hash;
			return this;
		}

		public TargetBuilder withChunks(int chunks) {
			return withChunks(Optional.of(chunks));
		}

		public TargetBuilder withChunks(Optional<Integer> chunks) {
			this.chunks = chunks;
			return this;
		}

		public TargetBuilder withUsername(String username) {
			return withUsername(Optional.ofNullable(username));
		}

		public TargetBuilder withRemoteFolderPath(String remoteFolder) {
			return withRemoteFolder(remoteFolder == null || remoteFolder.equals("") ? Optional.empty()
					: Optional.of(Path.of(remoteFolder)));
		}

		public TargetBuilder withRemoteFolderPath(Optional<String> remoteFolder) {
			return withRemoteFolder(remoteFolder.map(Path::of));
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

		public TargetBuilder withDisplayName(Optional<String> displayName) {
			this.displayName = displayName;
			return this;
		}

		public TargetBuilder withHostname(String hostname) {
			return withHostname(Optional.ofNullable(hostname));
		}

		public TargetBuilder withHostname(Optional<String> hostname) {
			this.hostname = hostname;
			return this;
		}

		public TargetBuilder withMode(Mode mode) {
			return withMode(Optional.ofNullable(mode));
		}

		public TargetBuilder withMode(Optional<Mode> mode) {
			this.mode = mode;
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

		public TargetBuilder withIdentityPath(String identity) {
			return withIdentity(
					identity == null || identity.equals("") ? Optional.empty() : Optional.of(Path.of(identity)));
		}

		public TargetBuilder withIdentityPath(Optional<String> identity) {
			return withIdentity(identity.map(Path::of));
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

	private final Optional<String> displayName;
	private final String username;
	private final String hostname;
	private final int port;
	private final boolean identities;
	private final boolean agent;
	private final boolean password;
	private final Optional<Path> identity;
	private final Optional<Path> remoteFolder;
	private final int authenticationTimeout;
	private final Mode mode;
	private final int chunks;
	private final boolean verifyIntegrity;
	private final boolean ignoreIntegrity;
	private final boolean multiplex;
	private final RemoteHash hash;

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
		this.mode = builder.mode.orElse(Mode.CHUNKED);
		this.chunks = builder.chunks.orElse(3);
		this.verifyIntegrity = builder.verifyIntegrity;
		this.ignoreIntegrity = builder.ignoreIntegrity;
		this.multiplex = builder.multiplex;
		this.hash = builder.hash.orElse(RemoteHash.sha512);
		this.displayName = builder.displayName;
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

	public Mode mode() {
		return mode;
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

	public int chunks() {
		return chunks;
	}

	public boolean verifyIntegrity() {
		return verifyIntegrity;
	}

	public boolean multiplex() {
		return multiplex;
	}

	public boolean ignoreIntegrity() {
		return ignoreIntegrity;
	}

	public RemoteHash hash() {
		return hash;
	}

	public Optional<String> displayName() {
		return displayName;
	}

	public String getDefaultDisplayName() {
		return String.format("%s@%s:%d/%s", this.username, this.hostname,
				this.port, this.remoteFolder.map(p -> p.toString().startsWith("/") ? p.toString().substring(1) : ("~/" + p.toString())).orElse("~"));
	}

}
