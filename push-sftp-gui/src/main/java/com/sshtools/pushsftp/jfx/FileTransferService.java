package com.sshtools.pushsftp.jfx;

import java.io.Closeable;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicLong;

import javax.json.Json;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sshtools.common.util.IOUtils;
import com.sshtools.pushsftp.jfx.PushSFTPUIApp.NotificationType;
import com.sshtools.pushsftp.jfx.SshTarget.SshTargetBuilder;
import com.sshtools.simjac.store.ConfigurationStoreBuilder;

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
	
	public enum TransferUnit {
		KB_S,
		MB_S,
		GB_S,
		KBITS_S,
		MBITS_S,
		GBITS_S,
		KIB_S,
		MIB_S,
		GIB_S,
		KIBITS_S,
		MIBITS_S,
		GIBITS_S
	}

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
			return rem == 0 ? 0 : (rem * 1000 / Math.max(1, bytesPerSecond()));
		}

		public String timeRemainingString() {
			var seconds = timeRemaining() / 1000;
			return String.format("%5d:%02d", (int) (seconds > 60 ? seconds / 60 : 0), (int) (seconds % 60));
		}
		
		public double secondsTaken() {
			return (double)timeTaken  / 1000d;
		}

		public long bytesPerSecond() {
			return timeTaken == 0 ? 0 :  (long)((double)transferred / secondsTaken());
		}

		public long bitsPerSecond() {
			return bytesPerSecond() * 8;
		}
		
		public double transferRate(TransferUnit unit) {
			switch(unit) {
			case KB_S:
				return (double)bytesPerSecond() / 1000D;
			case MB_S:
				return (double)bytesPerSecond() / 1000000D;
			case GB_S:
				return (double)bytesPerSecond() / 1000000000D;
			case KIB_S:
				return (double)bytesPerSecond() / 1024D;
			case MIB_S:
				return (double)bytesPerSecond() / 1048576D;
			case GIB_S:
				return (double)bytesPerSecond() / 1073741824D;
			case KBITS_S:
				return (double)bitsPerSecond() / 1000D;
			case MBITS_S:
				return (double)bitsPerSecond() / 1000000D;
			case GBITS_S:
				return (double)bitsPerSecond() / 1000000000D;
			case KIBITS_S:
				return (double)bitsPerSecond() / 1024D;
			case MIBITS_S:
				return (double)bitsPerSecond() / 1048576D;
			case GIBITS_S:
				return (double)bitsPerSecond() / 1073741824D;
			default:
				throw new UnsupportedOperationException();
			}
		}
		
		public String transferRateString() {
			return transferRateString(TransferUnit.MB_S);
		}
		
		public String transferRateString(TransferUnit unit) {
			return String.format("%.1fMibit/s", transferRate(unit));
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

	private final ObservableList<TargetJob<?, ?>> jobs = FXCollections.observableArrayList();
	private final ScheduledExecutorService service;
	private final ObservableList<Target> targets = loadTargets();
	private final ObservableList<Target> activeTargets = FXCollections.observableArrayList();
	private final Map<TargetJob<?, ?>, TransferStats> stats = new HashMap<>();
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

	public ObservableList<TargetJob<?, ?>> getJobs() {
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

	public void drop(Target target, List<Path> files) throws Exception {

		var prefs = context.getContainer().getAppPreferences();
		var taskBldr = target.createTransferTask(context);

		taskBldr.withVerbose(prefs.getBoolean("verbose", false))
		.withPaths(files)
		.withPassword(context.createPasswordPrompt(target))
		.withTarget(() -> {
			var idx = targets.indexOf(target);
			if(idx == -1)
				throw new IllegalStateException("Target has been removed.");
			else
				return targets.get(idx);
		})
		.withSerializer((t) -> {
			Platform.runLater(() -> {
				var idx = targets.indexOf(target);
				if(idx == -1) {
					targets.add(t);
				}
				else {
					targets.set(idx, t);
				}
			});
		})
		.withReporter(this);
		
		var task = taskBldr.build();
		
		task.setOnCancelled(evt -> {
			context.notification(NotificationType.ERROR, RESOURCES.getString("toast.cancelled.title"), //$NON-NLS-1$ //$NON-NLS-2$
					MessageFormat.format(RESOURCES.getString("toast.cancelled.text"), files.size(), target.uri()));
		});
		task.setOnFailed(evt -> {
			var e = task.exceptionNow();
			LOG.error("Failed.", e);
			context.notification(NotificationType.ERROR, RESOURCES.getString("toast.error.title"), //$NON-NLS-1$ //$NON-NLS-2$
					MessageFormat.format(RESOURCES.getString("toast.error.text"), files.size(), target.uri()));
		});
		task.setOnSucceeded(evt -> {
			context.notification(NotificationType.SUCCESS, RESOURCES.getString("toast.completed.title"), //$NON-NLS-1$ //$NON-NLS-2$
					MessageFormat.format(RESOURCES.getString("toast.completed.text"), files.size(), target.uri()));
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
					activeTargets.remove(target);
					jobs.remove(task);
					rebuildStats();
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

		var bldr = ConfigurationStoreBuilder.builder().
				withApp(PushSFTPUI.class).withName("targets").
				withoutFailOnMissingFile().
				withDeserializer((j) -> {
					targets.clear();
					var arr = j.asJsonArray();
					for (var el : arr) {
						targets.add(fromJsonObject(el.asJsonObject()));
					}
				}).
				withSerializer(() -> {
					var ob = Json.createArrayBuilder();
					for (var target : targets) {
						ob.add(target.toJsonObject());
					}
					return ob.build();
				});

		var store = bldr.build();
		store.retrieve();

		if (targets.isEmpty()) {
			targets.add(new SshTargetBuilder().build());
		}

		targets.addListener((ListChangeListener.Change<? extends Target> c) -> {
			store.store();
		});

		return targets;
	}

	private static Target fromJsonObject(JsonObject obj) {
		var type = obj.getString("type", SshTarget.class.getName());
		if(type.equals(SshTarget.class.getName())) {
			return SshTarget.fromJsonObject(obj);
		}
		else if(type.equals(HttpTarget.class.getName())) {
			return HttpTarget.fromJsonObject(obj);
		}
		else {
			throw new IllegalArgumentException("Unknown type " + type);
		}
	}

	@Override
	public void report(TargetJob<?,?> job, long length, long totalSoFar, long time) {
		Platform.runLater(() -> {
			stats.put(job, new TransferStats(time, length, totalSoFar));
			rebuildStats();
		});
	}
}
