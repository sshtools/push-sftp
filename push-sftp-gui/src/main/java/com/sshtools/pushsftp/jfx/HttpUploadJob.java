package com.sshtools.pushsftp.jfx;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.net.CookieManager;
import java.net.Socket;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509ExtendedTrustManager;

public final class HttpUploadJob extends TargetJob<Void, HttpTarget> {
	final static ResourceBundle RESOURCES = ResourceBundle.getBundle(HttpUploadJob.class.getName());
	
	static String boundary = new BigInteger(256, new Random()).toString();


	public final static class HttpUploadJobBuilder extends AbstractTargetJobBuilder<HttpTarget, HttpUploadJob, HttpUploadJobBuilder> implements TransferTaskBuilder<HttpUploadJobBuilder, HttpTarget, HttpUploadJob> {
		private final List<Path> files = new ArrayList<>();
		private Optional<Reporter> reporter = Optional.empty();

		public static HttpUploadJobBuilder builder() {
			return new HttpUploadJobBuilder();
		}

		public HttpUploadJobBuilder withReporter(Reporter reporter) {
			this.reporter = Optional.of(reporter);
			return this;
		}

		public HttpUploadJobBuilder withFiles(File... files) {
			return withFiles(Arrays.asList(files));
		}

		public HttpUploadJobBuilder withFiles(List<File> files) {
			this.files.clear();
			return addFiles(files);
		}

		public HttpUploadJobBuilder withPaths(Path... files) {
			return withPaths(Arrays.asList(files));
		}

		public HttpUploadJobBuilder withPaths(List<Path> files) {
			this.files.clear();
			return addPaths(files);
		}

		public HttpUploadJobBuilder addFiles(File... files) {
			return addFiles(Arrays.asList(files));
		}

		public HttpUploadJobBuilder addFiles(List<File> files) {
			this.files.addAll(files.stream().map(f -> f.toPath()).collect(Collectors.toList()));
			return this;
		}

		public HttpUploadJobBuilder addPaths(Path... files) {
			return addPaths(Arrays.asList(files));
		}

		public HttpUploadJobBuilder addPaths(List<Path> files) {
			this.files.addAll(files);
			return this;
		}

		public HttpUploadJob build() {
			return new HttpUploadJob(this);
		}
	}

	private final List<Path> files;
	private final Optional<Reporter> reporter;
	protected CookieManager cookies;

	private HttpUploadJob(HttpUploadJobBuilder builder) {
		super(builder);
		this.files = Collections.unmodifiableList(builder.files);
		this.reporter = builder.reporter;

		cookies = new CookieManager();
		updateMessage(RESOURCES.getString("waiting"));
	}


	@Override
	public Void call() throws Exception {
		
		var target = this.target.get();
		var uri = target.uri();
		var total = 0;
		var lst = new ArrayList<Path>();
		for(var file : files) {
			if(!Files.isDirectory(file)) {
				total += Files.size(file);
				lst.add(file);
			}
		}
		
		String message;
		if(lst.size() == 1)
			message = lst.get(0).toString();
		else
			message = MessageFormat.format(RESOURCES.getString("fileCount"), lst.size());
		updateMessage(MessageFormat.format(RESOURCES.getString("progress.uploading"), message));
		
		var started = System.currentTimeMillis();
		var ftotal = total;
		
		for(var file : files) {
			var filename = file.getFileName().toString();
			var request = HttpRequest.newBuilder().uri(uri)
					.header("Content-Type", "multipart/form-data;boundary=" + boundary)
					.POST(Multipart.ofParts(boundary, 
							Multipart.ofFile("file", file, filename, "application/octet-stream", (uploaded) -> {
								report(message, uploaded, ftotal, started);
							}),
							Multipart.ofFormData("name", target.name()),
							Multipart.ofFormData("email", target.email()),
							Multipart.ofFormData("reference", "Uploaded By FileDrop"), 
							Multipart.ofFormData("filename", filename)))
					.build();
	
			var res = newClient().send(request, HttpResponse.BodyHandlers.ofString());
			var rc = res.statusCode();
			if(rc < 200 || rc > 299) {
				throw new IOException("Unexpected response code " + rc);
			}
		}

		report(message, total, total, started);
		updateMessage(RESOURCES.getString("completed")); //$NON-NLS-1$
		return null;
	}
	
	@Override
	protected void failed() {
		var e = exceptionNow();
		updateMessage(MessageFormat.format(RESOURCES.getString("error.failedToUpload"), e, files.size(), //$NON-NLS-1$
				e.getMessage() == null ? "" : e.getMessage())); //$NON-NLS-1$
	}

	protected HttpClient newClient() throws NoSuchAlgorithmException, KeyManagementException {
		var b = HttpClient.newBuilder();
//		if (!isSecure()) {
			var ctx = SSLContext.getInstance("SSL");
			ctx.init(null, new TrustManager[] { new X509ExtendedTrustManager() {

				@Override
				public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
				}

				@Override
				public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
				}

				@Override
				public X509Certificate[] getAcceptedIssuers() {
					return null;
				}

				@Override
				public void checkClientTrusted(X509Certificate[] chain, String authType, Socket socket)
						throws CertificateException {
				}

				@Override
				public void checkServerTrusted(X509Certificate[] chain, String authType, Socket socket)
						throws CertificateException {
				}

				@Override
				public void checkClientTrusted(X509Certificate[] chain, String authType, SSLEngine engine)
						throws CertificateException {
				}

				@Override
				public void checkServerTrusted(X509Certificate[] chain, String authType, SSLEngine engine)
						throws CertificateException {
				}

			} }, new SecureRandom());
			var prms = new SSLParameters();
			prms.setEndpointIdentificationAlgorithm(null);
			b.sslParameters(prms);
			b.sslContext(ctx);
//		}
		b.cookieHandler(cookies);
		return b.build();
	}

	private boolean report(String name, long totalSoFar, long length, long started) {

		if (totalSoFar > 0) {
			var time = (System.currentTimeMillis() - started);
			updateProgress(totalSoFar, length);
			reporter.ifPresent(r -> r.report(this, length, totalSoFar, time));
		}
		return totalSoFar >= length;
	}
}
