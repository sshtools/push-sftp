package com.sshtools.pushsftp.jfx;

import static com.sshtools.jajafx.FXUtil.emptyPathIfBlankString;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Optional;

import javax.json.Json;
import javax.json.JsonObject;

import com.sshtools.client.sftp.RemoteHash;
import com.sshtools.pushsftp.jfx.PushJob.PushJobBuilder;

public final class SshTarget extends Target {

	public final static class SshTargetBuilder extends TargetBuilder<SshTargetBuilder> {

		private boolean identities = true;
		private boolean agent = true;
		private boolean password = true;
		private Optional<Path> identity = Optional.empty();
		private Optional<Path> preferredIdentity = Optional.empty();
		private int authenticationTimeout = 120;
		private Optional<Mode> mode = Optional.empty();
		private Optional<Integer> chunks = Optional.empty();
		private boolean verifyIntegrity;
		private boolean ignoreIntegrity;
		private boolean multiplex = false;
		private Optional<RemoteHash> hash = Optional.empty();
		private Optional<String> username = Optional.empty();
		private Optional<String> hostname = Optional.empty();
		private int port = 22;
		private Optional<String> unsafePassword = Optional.empty();
		private Optional<Path> remoteFolder = Optional.empty();
		
		public SshTargetBuilder fromTarget(SshTarget target) {
			if(target == null)
				return this;
			super.fromTarget(target);
			this.username = Optional.of(target.username);
			this.hostname = Optional.of(target.hostname);
			this.port = target.port;
			this.unsafePassword = target.unsafePassword;
			this.identities = target.identities;
			this.agent = target.agent;
			this.password = target.password;
			this.identity = target.identity;
			this.preferredIdentity = target.preferredIdentity;
			this.authenticationTimeout = target.authenticationTimeout;
			this.mode = Optional.of(target.mode);
			this.chunks = Optional.of(target.chunks);
			this.verifyIntegrity= target.verifyIntegrity;
			this.ignoreIntegrity= target.ignoreIntegrity;
			this.multiplex= target.multiplex;
			this.hash =Optional.of(target.hash);
			return this;
		}

		public final SshTargetBuilder withPort(int port) {
			this.port = port;
			return this;
		}

		public final SshTargetBuilder withUnsafePassword(String unsafePassword) {
			return withUnsafePassword(Optional.ofNullable(unsafePassword));
		}

		public final SshTargetBuilder withUnsafePassword(Optional<String> unsafePassword) {
			this.unsafePassword = unsafePassword;
			return this;
		}

		public final SshTargetBuilder withUsername(String username) {
			return withUsername(Optional.ofNullable(username));
		}

		public final SshTargetBuilder withRemoteFolderPath(String remoteFolder) {
			return withRemoteFolder(remoteFolder == null || remoteFolder.equals("") ? Optional.empty()
					: Optional.of(Path.of(remoteFolder)));
		}

		public final SshTargetBuilder withRemoteFolderPath(Optional<String> remoteFolder) {
			return withRemoteFolder(remoteFolder.map(Path::of));
		}

		public final SshTargetBuilder withRemoteFolder(Path remoteFolder) {
			return withRemoteFolder(Optional.of(remoteFolder));
		}

		public final SshTargetBuilder withRemoteFolder(Optional<Path> remoteFolder) {
			this.remoteFolder = remoteFolder;
			return this;
		}

		public final SshTargetBuilder withUsername(Optional<String> username) {
			this.username = username;
			return this;
		}

		public final SshTargetBuilder withHostname(String hostname) {
			return withHostname(Optional.ofNullable(hostname));
		}

		public final SshTargetBuilder withHostname(Optional<String> hostname) {
			this.hostname = hostname;
			return this;
		}

		public SshTargetBuilder withVerifyIntegrity(boolean verifyIntegrity) {
			this.verifyIntegrity = verifyIntegrity;
			return this;
		}

		public SshTargetBuilder withVerifiedIntegrity() {
			return withVerifyIntegrity(true);
		}

		public SshTargetBuilder withIgnoredIntegrity() {
			return withIgnoreIntegrity(true);
		}

		public SshTargetBuilder withIgnoreIntegrity(boolean ignoreIntegrity) {
			this.ignoreIntegrity = ignoreIntegrity;
			return this;
		}

		public SshTargetBuilder withMultiplex() {
			return withMultiplex(true);
		}

		public SshTargetBuilder withMultiplex(boolean multiplex) {
			this.multiplex= multiplex;
			return this;
		}


		public SshTargetBuilder withHash(RemoteHash hash) {
			return withHash(Optional.of(hash));
		}

		public SshTargetBuilder withHash(Optional<RemoteHash> hash) {
			this.hash = hash;
			return this;
		}

		public SshTargetBuilder withChunks(int chunks) {
			return withChunks(Optional.of(chunks));
		}

		public SshTargetBuilder withChunks(Optional<Integer> chunks) {
			this.chunks = chunks;
			return this;
		}

		public SshTargetBuilder withMode(Mode mode) {
			return withMode(Optional.ofNullable(mode));
		}

		public SshTargetBuilder withMode(Optional<Mode> mode) {
			this.mode = mode;
			return this;
		}

		public SshTargetBuilder withoutIdentities() {
			return withIdentities(false);
		}

		public SshTargetBuilder withIdentities(boolean identities) {
			this.identities = identities;
			return this;
		}

		public SshTargetBuilder withoutAgent() {
			return withAgent(false);
		}

		public SshTargetBuilder withAgent(boolean agent) {
			this.agent = agent;
			return this;
		}

		public SshTargetBuilder withoutPassword() {
			return withPassword(false);
		}

		public SshTargetBuilder withPassword(boolean password) {
			this.password = password;
			return this;
		}

		public SshTargetBuilder withIdentityPath(String identity) {
			return withIdentity(
					identity == null || identity.equals("") ? Optional.empty() : Optional.of(Path.of(identity)));
		}

		public SshTargetBuilder withIdentityPath(Optional<String> identity) {
			return withIdentity(identity.map(Path::of));
		}

		public SshTargetBuilder withIdentity(Path identity) {
			return withIdentity(Optional.of(identity));
		}

		public SshTargetBuilder withPreferredIdentity(Path preferredIdentity) {
			return withPreferredIdentity(Optional.of(preferredIdentity));
		}

		public SshTargetBuilder withPreferredIdentity(Optional<Path> preferredIdentity) {
			this.preferredIdentity = preferredIdentity;
			return this;
		}

		public SshTargetBuilder withIdentity(Optional<Path> identity) {
			this.identity = identity;
			return this;
		}

		public SshTargetBuilder withAuthenticationTimeout(int authenticationTimeout) {
			this.authenticationTimeout = authenticationTimeout;
			return this;
		}

		public SshTarget build() {
			return new SshTarget(this);
		}
	}

	private final boolean identities;
	private final boolean agent;
	private final boolean password;
	private final Optional<Path> identity;
	private final Optional<Path> preferredIdentity;
	private final int authenticationTimeout;
	private final Mode mode;
	private final int chunks;
	private final boolean verifyIntegrity;
	private final boolean ignoreIntegrity;
	private final boolean multiplex;
	private final RemoteHash hash;
	private final String username;
	private final String hostname;
	private final int port;
	private final Optional<Path> remoteFolder;
	private final Optional<String> unsafePassword;

	private SshTarget(SshTargetBuilder builder) {
		super(builder);
		this.username = builder.username.orElse(System.getProperty("user.name"));
		this.hostname = builder.hostname.orElse("localhost");
		this.port = builder.port;
		this.remoteFolder = builder.remoteFolder;
		this.unsafePassword = builder.unsafePassword;
		this.identities = builder.identities;
		this.agent = builder.agent;
		this.password = builder.password;
		this.identity = builder.identity;
		this.authenticationTimeout = builder.authenticationTimeout;
		this.mode = builder.mode.orElse(Mode.CHUNKED);
		this.chunks = builder.chunks.orElse(3);
		this.verifyIntegrity = builder.verifyIntegrity;
		this.ignoreIntegrity = builder.ignoreIntegrity;
		this.multiplex = builder.multiplex;
		this.hash = builder.hash.orElse(RemoteHash.sha512);
		this.preferredIdentity = builder.preferredIdentity;
	}

	public final String username() {
		return username;
	}

	public final Optional<Path> remoteFolder() {
		return remoteFolder;
	}

	public final String hostname() {
		return hostname;
	}

	public final int port() {
		return port;
	}

	public final Optional<String> unsafePassword() {
		return unsafePassword;
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

	public Optional<Path> preferredIdentity() {
		return preferredIdentity;
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

	@Override
	JsonObject toJsonObject() {
		var bldr = Json.createObjectBuilder();
		basics(bldr);
		bldr.add("hostname", hostname());
		bldr.add("username", username());
		unsafePassword().ifPresent(d -> bldr.add("unsafePassword", d));
		bldr.add("port", port());
		bldr.add("remoteFolder", remoteFolder().map(Path::toString).orElse(""));
		bldr.add("chunks", chunks());
		bldr.add("privateKey", identity().map(Path::toString).orElse(""));
		bldr.add("preferredPrivateKey", preferredIdentity().map(Path::toString).orElse(""));
		bldr.add("agentAuthentication", agent());
		bldr.add("passwordAuthentication", password());
		bldr.add("defaultIdentities", identities());
		bldr.add("mode", mode().name());
		bldr.add("verifyIntegrity", verifyIntegrity());
		bldr.add("multiplex", multiplex());
		bldr.add("ignoreIntegrity", ignoreIntegrity());
		bldr.add("authenticationTimeout", authenticationTimeout());
		bldr.add("hash", hash().name());
		return bldr.build();
	}

	@SuppressWarnings("unchecked")
	@Override
	protected <TARG extends Target, TSK extends TargetJob<?, TARG>, BLDR extends TransferTaskBuilder<BLDR, TARG, TSK>> BLDR createTransferTask(
			PushSFTPUIApp context) throws Exception {
		var prefs = context.getContainer().getAppPreferences();
		var agentSocket = prefs.get("agentSocket", "");
		return (BLDR) PushJobBuilder.builder()
				.withAgentSocket(agentSocket.equals("") ? Optional.empty() : Optional.of(agentSocket))
				.withHostKeyVerification(context.createHostKeyVerificationPrompt())
				.withPassphrasePrompt(context.createPassphrasePrompt(this));
	}

	public static SshTarget fromJsonObject(JsonObject obj) {
		return new SshTargetBuilder()
				.withHostname(obj.getString("hostname", ""))
				.withUsername(obj.getString("username", ""))
				.withPort(obj.getInt("port", 22))
				.withChunks(obj.getInt("chunks", 3))
				.withDisplayName(obj.getString("displayName", null))
				.withIdentity(emptyPathIfBlankString(obj.getString("privateKey", "")))
				.withPreferredIdentity(emptyPathIfBlankString(obj.getString("preferredPrivateKey", "")))
				.withRemoteFolder(emptyPathIfBlankString(obj.getString("remoteFolder", "")))
				.withAgent(obj.getBoolean("agentAuthentication", true))
				.withPassword(obj.getBoolean("passwordAuthentication", true))
				.withUnsafePassword(obj.getString("unsafePassword", null))
				.withIdentities(obj.getBoolean("defaultIdentities", true))
				.withMode(Mode.valueOf(obj.getString("mode", Mode.CHUNKED.name())))
				.withVerifyIntegrity(obj.getBoolean("verifyIntegrity", false))
				.withMultiplex(obj.getBoolean("multiplex", false))
				.withIgnoreIntegrity(obj.getBoolean("ignoreIntegrity", false))
				.withAuthenticationTimeout(obj.getInt("authenticationTimeout", 120))
				.withHash(RemoteHash.valueOf(obj.getString("hash", RemoteHash.sha512.name())))
				.build();
	}

	@Override
	public String getDefaultDisplayName() {
		return sshDisplayName(uri());
	}
	
	public static String sshDisplayName(URI uri) {
		return sshDisplayName(uri.getPort(), uri.getUserInfo(), uri.getHost(), uri.getPath());
	}

	public static String sshDisplayName(int port, String username, String host, String path) {
		var b = new StringBuilder();
		if(username != null && !username.equals(host)) {
			b.append(username);
			b.append("@");
		}
		b.append(host);
		if(port > -1 && port != 22) {
			b.append(":");
			b.append(port);
		}
		if(path != null && !path.equals("")) {
			b.append("/");
			b.append(path);
		}
		return b.toString();
	}

	@Override
	public URI uri() {
		try {
			return new URI("ssh", username, hostname, port, remoteFolder.map(Path::toString).orElse(null), null, null);
		} catch (URISyntaxException e) {
			throw new IllegalStateException(e);
		}
	}

}
