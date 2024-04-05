package com.sshtools.pushsftp.jfx;

import java.io.FileNotFoundException;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.BodyPublishers;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public class Multipart implements BodyPublisher {

	interface Part {
		String key();
	}

	static class FormDataPart implements BodyPublisher, Part {
		private BodyPublisher delegate;
		private String key;

		FormDataPart(String key, String value) {
			delegate = BodyPublishers.ofString(value);
			this.key = key;
		}

		@Override
		public void subscribe(Subscriber<? super ByteBuffer> subscriber) {
			delegate.subscribe(subscriber);
		}

		@Override
		public String key() {
			return key;
		}

		@Override
		public long contentLength() {
			return delegate.contentLength();
		}

	}

	static class FilePart implements BodyPublisher, Part {
		private final String contentType;
		private final String filename;
		private final BodyPublisher delegate;
		private final String key;
		private final Consumer<Long> onData;

		FilePart(String key, Path path, String filename, String contentType, Consumer<Long> onData) throws FileNotFoundException {
			this.key = key;
			this.contentType = contentType;
			this.filename = filename;
			delegate = BodyPublishers.ofFile(path);
			this.onData = onData;
		}

		@Override
		public void subscribe(Subscriber<? super ByteBuffer> subscriber) {
			delegate.subscribe(new Subscriber<ByteBuffer>() {
				
				AtomicLong total = new AtomicLong();

				@Override
				public void onSubscribe(Subscription subscription) {
					subscriber.onSubscribe(subscription);
				}

				@Override
				public void onNext(ByteBuffer item) {
					total.addAndGet(item.remaining());
					if(onData != null) {
						onData.accept(total.get());
					}
					subscriber.onNext(item);
				}

				@Override
				public void onError(Throwable throwable) {
					subscriber.onError(throwable);					
				}

				@Override
				public void onComplete() {
					subscriber.onComplete();
				}
			});
		}

		@Override
		public long contentLength() {
			return delegate.contentLength();
		}

		@Override
		public String key() {
			return key;
		}
	}

	public final static BodyPublisher ofFormData(String key, String value) {
		return new FormDataPart(key, value);
	}

	public final static BodyPublisher ofFile(String key, Path path) throws FileNotFoundException {
		return ofFile(key, path, "application/octet-stream");
	}

	public final static BodyPublisher ofFile(String key, Path path, String contentType) throws FileNotFoundException {
		return ofFile(key, path, path.getFileName().toString(), contentType);
	}

	public final static BodyPublisher ofFile(String key, Path path, String filename, String contentType)
			throws FileNotFoundException {
		return ofFile(key, path, filename, contentType, null);
	}

	public final static BodyPublisher ofFile(String key, Path path, String filename, String contentType, Consumer<Long> onData)
			throws FileNotFoundException {
		return new FilePart(key, path, filename, contentType, onData);
	}

	public final static BodyPublisher ofParts(String boundary, BodyPublisher... parts) {
		return new Multipart(boundary, parts);
	}

	private final static byte[] EOL = "\r\n".getBytes(StandardCharsets.UTF_8);

	private BodyPublisher delegate;

	private Multipart(String boundary, BodyPublisher... parts) {
		var separator = ("--" + boundary + "\r\nContent-Disposition: form-data; name=")
				.getBytes(StandardCharsets.UTF_8);
		var eof = ("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8);

		var allParts = new ArrayList<BodyPublisher>();
		for (var part : parts) {
			if (!(part instanceof Part)) {
				throw new IllegalArgumentException(String.format(
						"Each publisher must implement %s, %s does not. Use %s.ofFile() and %s.ofFormData() methods to construct publishes for multipart content.",
						Part.class.getName(), part.getClass().getName(), Multipart.class.getName(), Multipart.class.getName()));
			}
			allParts.add(BodyPublishers.ofByteArray(separator));
			var key = ((Part) part).key();
			String filename = null;
			String contentType = null;
			if (part instanceof FilePart) {
				filename = ((FilePart) part).filename;
				contentType = ((FilePart) part).contentType;
			}
			var b = new StringBuilder();
			b.append("\"");
			b.append(key);
			b.append("\"");
			if (filename != null) {
				b.append("; filename=\"");
				b.append(filename);
				b.append("\"");
			}
			b.append("\r\n");
			if (contentType != null) {
				b.append("Content-Type: ");
				b.append(contentType);
				b.append("\r\n");
			}
			b.append("\r\n");
			allParts.add(BodyPublishers.ofString(b.toString()));
			allParts.add(part);
			allParts.add(BodyPublishers.ofByteArray(EOL));
		}
		allParts.add(BodyPublishers.ofByteArray(eof));
		delegate = BodyPublishers.concat(allParts.toArray(new BodyPublisher[0]));
	}

	@Override
	public void subscribe(Subscriber<? super ByteBuffer> subscriber) {
		delegate.subscribe(subscriber);
	}

	@Override
	public long contentLength() {
		return delegate.contentLength();
	}
}
