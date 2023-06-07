package com.sshtools.pushsftp.jfx;

import static com.sshtools.jajafx.FXUtil.emptyPathIfBlankString;

import java.io.Closeable;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicLong;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonValue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sshtools.client.sftp.RemoteHash;
import com.sshtools.common.util.IOUtils;
import com.sshtools.pushsftp.jfx.PushJob.PushJobBuilder;
import com.sshtools.pushsftp.jfx.PushJob.Reporter;
import com.sshtools.pushsftp.jfx.PushSFTPUIApp.NotificationType;
import com.sshtools.pushsftp.jfx.Target.TargetBuilder;
import com.sshtools.simjac.ConfigurationStoreBuilder;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

public class FileTransferService implements Closeable, Reporter {
	final static Logger LOG = LoggerFactory.getLogger(FileTransferService.class);
	final static ResourceBundle RESOURCES = ResourceBundle.getBundle(FileTransferService.class.getName());

	public static final class TransferStats {
		private final long timeTaken;
		private final long size;
		private final long transferred;

		TransferStats() {
			this(0, 0, 0);
		}

		TransferStats(long timeTaken, long size, long transferred) {
			super();
			this.timeTaken = timeTaken;
			this.size = size;
			this.transferred = transferred;
		}

		public long transferred() {
			return transferred;
		}

		public long size() {
			return size;
		}

		public long timeTaken() {
			return timeTaken;
		}

		public long remaining() {
			return size - transferred;
		}

		public long timeRemaining() {
			var rem = remaining();
			return rem == 0 ? 0 : (rem / Math.max(1, bytesPerSecond()));
		}

		public String timeRemainingString() {
			var seconds = timeRemaining() / 1000;
			return String.format("%5d:%02d", (int) (seconds > 60 ? seconds / 60 : 0), (int) (seconds % 60));
		}

		public long bytesPerSecond() {
			return timeTaken == 0 ? 0 : transferred / timeTaken;
		}

		public String transferRateString() {
			return String.format("%.1fMibit/s", bytesPerSecond() / 1024D);
		}

		public String percentageString() {
			return String.format("%.0f%%", percentage()); //$NON-NLS-1$
		}

		public String humanBytes() {
			return IOUtils.toByteSize(transferred);
		}

		public double percentage() {
			return size == 0 ? 0 : ((double) transferred / (double) size) * 100;
		}

	}

	private final ObservableList<PushJob> jobs = FXCollections.observableArrayList();
	private final ScheduledExecutorService service;
	private final ObservableList<Target> targets = loadTargets();
	private final ObservableList<Target> activeTargets = FXCollections.observableArrayList();
	private final Map<PushJob, TransferStats> stats = new HashMap<>();
	private final ObjectProperty<TransferStats> summary = new SimpleObjectProperty<>(new TransferStats());
	private final PushSFTPUIApp context;
	private final BooleanProperty busy;
	private final IntegerProperty active;

	public FileTransferService(PushSFTPUIApp context) {
		service = Executors.newSingleThreadScheduledExecutor();
		this.context = context;
		busy = new SimpleBooleanProperty();
		busy.bind(Bindings.isNotEmpty(activeTargets));
		active = new SimpleIntegerProperty();
		active.bind(Bindings.size(activeTargets));
	}

	public ReadOnlyObjectProperty<TransferStats> summaryProperty() {
		return summary;
	}

	public ObservableList<PushJob> getJobs() {
		return jobs;
	}

	public ReadOnlyBooleanProperty busyProperty() {
		return busy;
	}

	public ReadOnlyIntegerProperty activeProperty() {
		return active;
	}

	public final ObservableList<Target> getTargets() {
		return targets;
	}

	public void drop(Target target, List<Path> files) {

		var prefs = context.getContainer().getAppPreferences();
		var agentSocket = prefs.get("agentSocket", "");

		var task = PushJobBuilder.builder().
				withVerbose(prefs.getBoolean("verbose", false))
				.withAgentSocket(agentSocket.equals("") ? Optional.empty() : Optional.of(agentSocket))
				.withPaths(files)
				.withTarget(target)
				.withReporter(this)
				.withPassphrasePrompt(context.createPassphrasePrompt(target))
				.withPassword(context.createPasswordPrompt(target)).build();
		
		task.setOnFailed(evt -> {
			var e = task.exceptionNow();
			LOG.error("Failed.", e);
			context.notification(NotificationType.ERROR, RESOURCES.getString("toast.error.title"), //$NON-NLS-1$ //$NON-NLS-2$
					MessageFormat.format(RESOURCES.getString("toast.error.text"), files.size(), target.username(),
							target.hostname(), target.port(), target.remoteFolder().map(Path::toString).orElse("")));
		});
		task.setOnSucceeded(evt -> {
			context.notification(NotificationType.SUCCESS, RESOURCES.getString("toast.completed.title"), //$NON-NLS-1$ //$NON-NLS-2$
					MessageFormat.format(RESOURCES.getString("toast.completed.text"), files.size(), target.username(),
							target.hostname(), target.port(), target.remoteFolder().map(Path::toString).orElse("")));
		});
		
		jobs.add(task);
		var stat = new TransferStats();
		stats.put(task, stat);
		rebuildStats();
		service.submit(() -> {
			try {
				Platform.runLater(() -> {
					activeTargets.add(target);
				});
				task.run();
				return task.resultNow();
			} finally {
				Platform.runLater(() -> {
					stats.remove(task);
					rebuildStats();
					activeTargets.remove(target);
					jobs.remove(task);
				});
			}
		});
	}

	public boolean isActive(Target target) {
		return activeTargets.contains(target);
	}

	@Override
	public void close() {
		service.shutdown();
	}

	private void rebuildStats() {
		var size = new AtomicLong();
		var timeTake = new AtomicLong();
		var transferred = new AtomicLong();
		stats.forEach((k, v) -> {
			size.addAndGet(v.size);
			timeTake.addAndGet(v.timeTaken);
			transferred.addAndGet(v.transferred);
		});
		summary.set(new TransferStats(timeTake.longValue(), size.longValue(), transferred.longValue()));

	}

	private static ObservableList<Target> loadTargets() {
		ObservableList<Target> targets = FXCollections.observableArrayList();

//		var bldr = ConfigurationStoreBuilder.builder().
//			withApp(PushSFTPUI.class).
//			withName("targets").
//			withoutFailOnMissingFile().
//			withBinding(ArrayBindingBuilder.builder(Target.class, targets).
//				withBindingBuilder(() -> TargetBuilder.builder(), (b) -> b.build(), (b) -> 
//					ObjectBindingBuilder.builder(Target.class).withBinding(
//						xstring("hostname", b::withHostname, Target.class, Target::hostname).build(),
//						xstring("username", b::withUsername, Target.class, Target::username).build(),
//						xstring("remoteFolder", b::withRemoteFolderPath, Target.class, t -> t.remoteFolder().map(Path::toString).orElse("")).build(),
//						xstring("privateKey", b::withIdentityPath, Target.class, t -> t.remoteFolder().map(Path::toString).orElse("")).build(), 
//						xinteger("port", b::withPort, Target.class, Target::port).build(),
//						xboolean("agentAuthentication", b::withAgent).build(),
//						xboolean("defaultIdentities", b::withIdentities).build(),
//						xobject(Mode.class, "mode", b::withMode).build(),
//						xboolean("passwordAuthentication", b::withPassword).build(), 
//						xinteger("chunks", b::withChunks, Target.class, Target::chunks).build(),
//						xboolean("verifyIntegrity", b::withVerifyIntegrity, Target.class, Target::verifyIntegrity).build(),
//						xboolean("ignoreIntegrity", b::withIgnoreIntegrity, Target.class, Target::ignoreIntegrity).build(),
//						xboolean("preAllocate", b::withPreAllocate, Target.class, Target::preAllocate).build(),
//						xboolean("copyDataExtension", b::withCopyDataExtension, Target.class, Target::copyDataExtension).build(),
//						xobject(RemoteHash.class, "hash", b::withHash, Target.class, Target::hash).build()
//						).build()
//				).
//				build());
//		
//		var store = bldr.build();
//		store.retrieve();

		var bldr = ConfigurationStoreBuilder.builder().withApp(PushSFTPUI.class).withName("targets")
				.withoutFailOnMissingFile().withDeserializer((j) -> {
					targets.clear();
					var arr = j.asJsonArray();
					for (var el : arr) {
						targets.add(fromJsonObject(el.asJsonObject()));
					}
				}).withSerializer(() -> {
					var ob = Json.createArrayBuilder();
					for (var target : targets) {
						ob.add(toJsonObject(target));
					}
					return ob.build();
				});

		var store = bldr.build();
		store.retrieve();

		if (targets.isEmpty()) {
			targets.add(TargetBuilder.builder().build());
		}

		targets.addListener((ListChangeListener.Change<? extends Target> c) -> {
			store.store();
		});

		return targets;
	}

	private static Target fromJsonObject(JsonObject obj) {
		return TargetBuilder.builder().withHostname(obj.getString("hostname", ""))
				.withUsername(obj.getString("username", "")).withPort(obj.getInt("port", 22))
				.withChunks(obj.getInt("chunks", 3))
				.withDisplayName(obj.getString("displayName", null))
				.withIdentity(emptyPathIfBlankString(obj.getString("privateKey", "")))
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
				.withHash(RemoteHash.valueOf(obj.getString("hash", RemoteHash.sha512.name()))).build();
	}

	private static JsonValue toJsonObject(Target target) {
		var bldr = Json.createObjectBuilder();
		bldr.add("hostname", target.hostname());
		bldr.add("username", target.username());
		target.displayName().ifPresent(d -> bldr.add("displayName", d));
		target.unsafePassword().ifPresent(d -> bldr.add("unsafePassword", d));
		bldr.add("port", target.port());
		bldr.add("chunks", target.chunks());
		bldr.add("privateKey", target.identity().map(Path::toString).orElse(""));
		bldr.add("remoteFolder", target.remoteFolder().map(Path::toString).orElse(""));
		bldr.add("agentAuthentication", target.agent());
		bldr.add("passwordAuthentication", target.password());
		bldr.add("defaultIdentities", target.identities());
		bldr.add("mode", target.mode().name());
		bldr.add("verifyIntegrity", target.verifyIntegrity());
		bldr.add("multiplex", target.multiplex());
		bldr.add("ignoreIntegrity", target.ignoreIntegrity());
		bldr.add("authenticationTimeout", target.authenticationTimeout());
		bldr.add("hash", target.hash().name());
		return bldr.build();
	}

	@Override
	public void report(PushJob job, long length, long totalSoFar, long time) {
		Platform.runLater(() -> {
			stats.put(job, new TransferStats(time, length, totalSoFar));
			rebuildStats();
		});
	}
}
