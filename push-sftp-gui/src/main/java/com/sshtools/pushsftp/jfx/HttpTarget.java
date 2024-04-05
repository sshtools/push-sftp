package com.sshtools.pushsftp.jfx;

import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Optional;

import javax.json.Json;
import javax.json.JsonObject;

import com.sshtools.pushsftp.jfx.HttpUploadJob.HttpUploadJobBuilder;

public final class HttpTarget extends Target {

	public final static class HttpTargetBuilder extends TargetBuilder<HttpTargetBuilder> {
		
		private Optional<URL> url = Optional.empty();
		private Optional<String> email = Optional.empty();
		private Optional<String> name = Optional.empty();

		public HttpTargetBuilder withName(String name) {
			this.name = "".equals(name) ? Optional.empty() : Optional.of(name);
			return this;
		}

		public HttpTargetBuilder withEmail(String email) {
			this.email = "".equals(email) ? Optional.empty() : Optional.of(email);
			return this;
		}

		public HttpTargetBuilder withUrl(String url) {
			try {
				return withUrl(new URL(url));
			} catch (MalformedURLException e) {
				throw new UncheckedIOException(e);
			}
		}
		
		public HttpTargetBuilder withUrl(URL url) {
			this.url = Optional.of(url);
			return this;
		}

		public HttpTarget build() {
			return new HttpTarget(this);
		}
	}
	
	private final Optional<URL> url;
	private final Optional<String> name;
	private final Optional<String> email;

	private HttpTarget(HttpTargetBuilder builder) {
		super(builder);
		this.url = builder.url;
		this.name = builder.name;
		this.email = builder.email;
	}

	@Override
	JsonObject toJsonObject() {
		var bldr = Json.createObjectBuilder();
		basics(bldr);
		bldr.add("url", uri().toString());
		bldr.add("name", name());
		bldr.add("email", email());
		return bldr.build();
	}


	public static HttpTarget fromJsonObject(JsonObject obj) {
		return new HttpTargetBuilder()
				.withUrl(obj.getString("url"))
				.withName(obj.getString("name"))
				.withEmail(obj.getString("email"))
				.withDisplayName(obj.getString("displayName", null))
				.build();
	}

	@SuppressWarnings("unchecked")
	@Override
	protected <TARG extends Target, TSK extends TargetJob<?, TARG>, BLDR extends TransferTaskBuilder<BLDR, TARG, TSK>> BLDR createTransferTask(
			PushSFTPUIApp context) throws Exception {
		return (BLDR) new HttpUploadJobBuilder();
	}

	@Override
	public URI uri() {
			return url.map(t -> {
				try {
					return t.toURI();
				} catch (URISyntaxException e) {
					throw new IllegalStateException(e);
				}
			}).orElse(null);
	}

	public String name() {
		return name.orElse("");
	}

	public String email() {
		return email.orElse("");
	}

	@Override
	public String getDefaultDisplayName() {
		return httpDisplayName(uri());
	}
	
	public static String httpDisplayName(URI uri) {
		return httpDisplayName(uri.getPort(), uri.getUserInfo(), uri.getHost(), uri.getPath());
	}

	public static String httpDisplayName(int port, String username, String host, String path) {
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
}
