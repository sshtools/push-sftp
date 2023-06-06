package com.sshtools.pushsftp.jfx;

import static com.sshtools.jajafx.FXUtil.emptyPathIfBlankString;

import java.io.Closeable;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonValue;

import com.sshtools.client.sftp.RemoteHash;
import com.sshtools.pushsftp.jfx.PushJob.PushJobBuilder;
import com.sshtools.pushsftp.jfx.Target.TargetBuilder;
import com.sshtools.sequins.Progress;
import com.sshtools.simjac.ConfigurationStoreBuilder;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

public class FileTransferService implements Closeable {

	private final ObservableList<Callable<Void>> jobs = FXCollections.observableArrayList();
	private final ScheduledExecutorService service;
	private final ObservableList<Target> targets = loadTargets();
	private final ObservableList<Target> activeTargets = FXCollections.observableArrayList();
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

	public ReadOnlyBooleanProperty busyProperty() {
		return busy;
	}

	public ReadOnlyIntegerProperty activeProperty() {
		return active;
	}

	public final ObservableList<Target> getTargets() {
		return targets;
	}

	public void drop(Progress progress, Target target, List<Path> files) {

		var prefs = context.getContainer().getAppPreferences();
		var agentSocket = prefs.get("agentSocket", "");

		submit(target, PushJobBuilder.builder().withVerbose(prefs.getBoolean("verbose", false))
				.withAgentSocket(agentSocket.equals("") ? Optional.empty() : Optional.of(agentSocket))
				.withProgress(progress).withPaths(files).withTarget(target)
				.withPassphrasePrompt(context.createPassphrasePrompt(target))
				.withPassword(context.createPasswordPrompt(target)).build());
	}
	
	public boolean isActive(Target target) {
		return activeTargets.contains(target);
	}

	private void submit(Target target, Callable<Void> task) {
		jobs.add(task);
		service.submit(() -> {
			try {
				Platform.runLater(() -> {
					activeTargets.add(target);
				});
				task.call();
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				Platform.runLater(() -> {
					activeTargets.remove(target);
					jobs.remove(task);
				});
			}
		});
	}

	@Override
	public void close() {
		service.shutdown();
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
				.withIdentity(emptyPathIfBlankString(obj.getString("privateKey", "")))
				.withRemoteFolder(emptyPathIfBlankString(obj.getString("remoteFolder", "")))
				.withAgent(obj.getBoolean("agentAuthentication", true))
				.withPassword(obj.getBoolean("passwordAuthentication", true))
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
}
