package com.sshtools.pushsftp.jfx;

import java.net.URI;
import java.util.Objects;
import java.util.Optional;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

public abstract class Target {

	public abstract static class TargetBuilder<BLDR extends TargetBuilder<BLDR>> {

		private Optional<String> displayName = Optional.empty();

		@SuppressWarnings("unchecked")
		public BLDR fromTarget(Target target) {
			if (target == null)
				return (BLDR) this;
			this.displayName = target.displayName;
			return (BLDR) this;
		}

		public final BLDR withDisplayName(String displayName) {
			return withDisplayName(Optional.ofNullable(displayName));
		}

		@SuppressWarnings("unchecked")
		public final BLDR withDisplayName(Optional<String> displayName) {
			this.displayName = displayName;
			return (BLDR) this;
		}

	}

	private final Optional<String> displayName;

	protected Target(TargetBuilder<?> builder) {
		this.displayName = builder.displayName;
	}

	@Override
	public int hashCode() {
		return Objects.hash(uri());
	}
	
	public abstract URI uri();

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Target other = (Target) obj;
		return Objects.equals(uri(), other.uri());
	}

	public final Optional<String> displayName() {
		return displayName;
	}

	public final String bestDisplayName() {
		return displayName.orElse(getDefaultDisplayName());
	}

	public String getDefaultDisplayName() {
//		return String.format("%s@%s:%d/%s", this.username, this.hostname, this.port,
//				this.remoteFolder
//						.map(p -> p.toString().startsWith("/") ? p.toString().substring(1) : ("~/" + p.toString()))
//						.orElse("~"));
		return uri().toString();
	}

	abstract JsonObject toJsonObject();
	
	void basics(JsonObjectBuilder bldr) {
		bldr.add("type", getClass().getName());
		displayName().ifPresent(d -> bldr.add("displayName", d));
	}

	protected abstract <TARG extends Target, TSK extends TargetJob<?, TARG>, BLDR extends TransferTaskBuilder<BLDR, TARG, TSK>> BLDR createTransferTask(PushSFTPUIApp context) throws Exception;

}
