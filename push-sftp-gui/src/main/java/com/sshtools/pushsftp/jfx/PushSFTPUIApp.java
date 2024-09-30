package com.sshtools.pushsftp.jfx;

import static com.sshtools.jajafx.FXUtil.maybeQueue;

import java.io.IOException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.text.MessageFormat;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javakeyring.BackendNotSupportedException;
import com.github.javakeyring.Keyring;
import com.github.javakeyring.PasswordAccessException;
import com.sshtools.client.ClientAuthenticator;
import com.sshtools.client.PassphrasePrompt;
import com.sshtools.client.PasswordAuthenticator.PasswordPrompt;
import com.sshtools.common.knownhosts.HostKeyVerification;
import com.sshtools.common.knownhosts.KnownHostsFile;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.SshPublicKey;
import com.sshtools.jajafx.AboutPage;
import com.sshtools.jajafx.JajaApp;
import com.sshtools.jajafx.JajaFXApp;
import com.sshtools.jajafx.JajaFXAppWindow;
import com.sshtools.jajafx.Tiles;
import com.sshtools.jajafx.UpdatePage;
import com.sshtools.jajafx.progress.PasswordPage;
import com.sshtools.jajafx.progress.YesNoPage;
import com.sshtools.twoslices.BasicToastHint;
import com.sshtools.twoslices.Toast;
import com.sshtools.twoslices.ToastType;
import com.sshtools.twoslices.ToasterFactory;
import com.sshtools.twoslices.ToasterSettings.SystemTrayIconMode;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.stage.Stage;

public class PushSFTPUIApp extends JajaFXApp<PushSFTPUI, JajaFXAppWindow<PushSFTPUIApp>> {
	
	public enum NotificationType {
		ERROR,
		INFO,
		NONE,
		WARNING,
		SUCCESS;
	}

	final static ResourceBundle RESOURCES = ResourceBundle.getBundle(PushSFTPUIApp.class.getName());
	final static Logger LOG = LoggerFactory.getLogger(JajaApp.class);

	private final FileTransferService service;
	private final Optional<Keyring> keyring;

	private Tiles<PushSFTPUIApp> tiles;
	private HostKeyVerification hkv;

	public PushSFTPUIApp() {
		super(PushSFTPUIApp.class.getResource("icon.png"), RESOURCES.getString("title"), (PushSFTPUI) PushSFTPUI.getInstance(), ((PushSFTPUI) PushSFTPUI.getInstance()).getAppPreferences());
		service = new FileTransferService(this);
		
		Optional<Keyring> k;
		try {
			k = Optional.of(Keyring.create());
		} catch (BackendNotSupportedException e) {
			LOG.warn("No keyrings supported.", e);
			k = Optional.empty();
		}
		keyring = k;
		
		configureToaster();
	}

	@Override
	protected JajaFXAppWindow createAppWindow(Stage stage) {
		var wnd =  new JajaFXAppWindow(stage, this, 480, 660);
		wnd.setContent(createContent(stage, wnd));
		return wnd;
	}

	private void configureToaster() {
		var settings = ToasterFactory.getSettings();
		var properties = settings.getHints();
		settings.setAppName(RESOURCES.getString("title")); 
		settings.setSystemTrayIconMode(SystemTrayIconMode.HIDDEN);
		if( System.getProperty("os.name").toLowerCase().contains("mac os") || System.getProperty("os.name").toLowerCase().contains("windows")) {
			settings.setPreferredToasterClassName("com.sshtools.twoslices.impl.JavaFXToaster");
		}
		properties.put(BasicToastHint.DARK, isDarkMode());
		ObservableList<String> l = FXCollections.observableArrayList();
		addCommonStylesheets(l);
		properties.put(BasicToastHint.STYLESHEETS, l);
		properties.put(BasicToastHint.COLLAPSE_MESSAGE, RESOURCES.getString("collapse"));
		properties.put(BasicToastHint.THRESHOLD, 6);
		properties.put(BasicToastHint.TYPE_ICON_GENERATOR, new Function<ToastType, Node>() {
			@Override
			public Node apply(ToastType t) {
				Node node = createNode(t);
				if(node != null)
					node.getStyleClass().addAll("icon-" + toTextName(t), "padded");
				return node;
			}
			
			private String toTextName(ToastType t) {
				switch(t) {
				case ERROR:
					return "danger";
				default:
					return t.name().toLowerCase();
				}
			}

			private Node createNode(ToastType t) {
				switch(t) {
				case ERROR:
					return FontIcon.of(FontAwesomeSolid.EXCLAMATION_CIRCLE, 48);
				case WARNING:
					return FontIcon.of(FontAwesomeSolid.EXCLAMATION_TRIANGLE, 48);
				case INFO:
					return FontIcon.of(FontAwesomeSolid.INFO_CIRCLE, 48);
				default:
					return null;
				}
			}
		});
	}

	public final Tiles<PushSFTPUIApp> getTiles() {
		return tiles;
	}

	public static void main(String[] args) {
		launch(args);
	}

	public FileTransferService getService() {
		return service;
	}

	public boolean isKeyringAvailable() {
		return keyring.isPresent();
	}

	@Override
	public void needUpdate() {
		maybeQueue(() -> {
			if (!(tiles.getCurrentPage() instanceof AboutPage) && !(tiles.getCurrentPage() instanceof UpdatePage)
					&& !service.busyProperty().get()) {
				tiles.popup(UpdatePage.class);
			}
		});
	}

	public HostKeyVerification createHostKeyVerificationPrompt() throws SshException, IOException {
		if(hkv == null)
			hkv = createHostKeyVerificationPromptImpl();
		return hkv;
	}

	public HostKeyVerification createHostKeyVerificationPromptImpl() throws IOException, SshException {
		var file = KnownHostsFile.defaultKnownHostsFile();
		if(!Files.exists(file))
			Files.createFile(file);
		return new KnownHostsFile() {
			{
				setHashHosts(true);
				setUseCanonicalHostnames(true);
				setUseReverseDNS(false);
			}
			
			@Override
			protected void onUnknownHost(String host, SshPublicKey key) throws SshException {
				try {
					unknownHost(host, key).ifPresent(always -> {
						try {
							allowHost(host, key, always);
						} catch (SshException e) {
							throw new IllegalStateException("Failed to save known host.");
						}
					});
				} catch (UnknownHostException e) {
					throw new SshException(e);
				} 
			}

			@Override
			protected void onHostKeyMismatch(String host, List<SshPublicKey> allowedHostKey, SshPublicKey actualHostKey)
					throws SshException {
				try {
					if(mismatchedHost(host, allowedHostKey, actualHostKey)) {
						try {
							for(var key : allowedHostKey) {
								removeEntries(key);
							}
							allowHost(host, actualHostKey, true);
						} catch (SshException e) {
							throw new IllegalStateException("Failed to save known host.");
						}
					}
				} catch (UnknownHostException e) {
					throw new SshException(e);
				} 
			}
			
		};
	}

	public PassphrasePrompt createPassphrasePrompt(SshTarget target) {
		var save = new AtomicBoolean();
		return new PassphrasePrompt() {
			@Override
			public String getPasshrase(String keyinfo) {
				try {
					if (keyring.isEmpty())
						throw new PasswordAccessException("No keyring.");
					return keyring.get().getPassword(keyinfo, target.username());
				} catch (PasswordAccessException pae) {
					return password(save, target.username(), RESOURCES.getString("passphraseDialog.title"));
				}
			}

			@Override
			public void completed(boolean success, String value, ClientAuthenticator authenticator) {
				// TODO need store against keyinfo?
//				if(keyring != null) {
//					// TODO ideally we should only save or delete if the prompt was actually used
//					try {
//						if(success) {
//				if(save.get())
//							keyring.setPassword(serviceName, target.username(), value);
//						}
//						else {
//							keyring.deletePassword(serviceName, target.username());
//						}
//					}
//					catch(PasswordAccessException pae) {
//					}
//				}
			}
		};
	}

	public PasswordPrompt createPasswordPrompt(Target target) {
		var save = new AtomicBoolean();
		var serviceName = target.uri().toString();
		var username = target.uri().getUserInfo() == null ? "public" : target.uri().getUserInfo();
		return new PasswordPrompt() {

			@Override
			public String get() {
				try {
					if (keyring.isEmpty())
						throw new PasswordAccessException("No keyring.");
					return keyring.get().getPassword(serviceName, username);
				} catch (PasswordAccessException pae) {
					return password(save, username, RESOURCES.getString("passwordDialog.title"));
				}
			}

			@Override
			public void completed(boolean success, String value, ClientAuthenticator authenticator) {
				keyring.ifPresent(k -> {
					try {
						if (success) {
							if (save.get())
								k.setPassword(serviceName, username, value);
						} else {
							k.deletePassword(serviceName, username);
						}
					} catch (PasswordAccessException pae) {
					}
				});
			}
		};
	}

	private boolean mismatchedHost(String host, List<SshPublicKey> allowedHostKey, SshPublicKey actualHostKey) throws SshException, UnknownHostException {
		var sem = new Semaphore(1);
		var result = new AtomicBoolean();
		var others = String.join("\n", allowedHostKey.stream().map(t -> {
			try {
				return t.getFingerprint();
			} catch (SshException e) {
				throw new IllegalStateException(e);
			}
		}).collect(Collectors.toList()));
		var txt = MessageFormat.format(RESOURCES.getString("mismatchedHost.content"), 
				host, 
				actualHostKey.getJCEPublicKey().getAlgorithm(), actualHostKey.getFingerprint(),
				others);
		
		try {
			sem.acquire();
			Platform.runLater(() -> {
				@SuppressWarnings("unchecked")
				var confirmPage = (YesNoPage<PushSFTPUIApp>) tiles.popup(YesNoPage.class);
				confirmPage.preferNo();
				confirmPage.titleText().set(RESOURCES.getString("mismatchedHost"));
				confirmPage.textText().set(txt);
				confirmPage.onYes((e) -> {
					tiles.remove(confirmPage);
					result.set(true);
					sem.release();
				});
				confirmPage.onNo((e) -> {
					tiles.remove(confirmPage);
					result.set(false);
					sem.release();
				});
			});
			sem.acquire();
		} catch (InterruptedException ie) {
			throw new IllegalStateException("Interrupted.", ie);
		} finally {
			sem.release();
		}
		return result.get();
	}

	private Optional<Boolean> unknownHost(String host, SshPublicKey key) throws SshException, UnknownHostException {
		var sem = new Semaphore(1);
		var remember = new AtomicBoolean(false);
		var result = new AtomicBoolean();
		var txt = MessageFormat.format(RESOURCES.getString("unknownHost.content"), 
				host, 
				key.getJCEPublicKey().getAlgorithm(), key.getFingerprint());
		try {
			sem.acquire();
			Platform.runLater(() -> {
				@SuppressWarnings("unchecked")
				var confirmPage = (YesNoPage<PushSFTPUIApp>) tiles.popup(YesNoPage.class);
				confirmPage.titleText().set(RESOURCES.getString("unknownHost"));
				
				var always = new Button(RESOURCES.getString("always"));
				always.getStyleClass().add("btn-success");
				always.setOnAction(evt -> {
					tiles.remove(confirmPage);
					result.set(true);
					remember.set(true);
					sem.release();
				});
				confirmPage.accessories().add(always);
				
				confirmPage.textText().set(txt);
				confirmPage.onYes((e) -> {
					tiles.remove(confirmPage);
					result.set(true);
					sem.release();
				});
				confirmPage.onNo((e) -> {
					tiles.remove(confirmPage);
					result.set(false);
					sem.release();
				});
			});
			sem.acquire();
		} catch (InterruptedException ie) {
			throw new IllegalStateException("Interrupted.", ie);
		} finally {
			sem.release();
		}
		return result.get() ? Optional.of(remember.get()) : Optional.empty();
	}

	private String password(AtomicBoolean save, String username, String fmt, Object... args) {
		var sem = new Semaphore(1);
		var buf = new StringBuilder();
		try {
			sem.acquire();
			Platform.runLater(() -> {
				@SuppressWarnings("unchecked")
				var passwordPage = (PasswordPage<PushSFTPUIApp>) tiles.popup(PasswordPage.class);
				var txt = MessageFormat.format(fmt, args);
				passwordPage.titleText().set(txt);
				passwordPage.textText()
						.set(MessageFormat.format(RESOURCES.getString("passwordDialog.prompt"), username));
				passwordPage.onConfirm((e) -> {
					save.set(passwordPage.isSave());
					buf.append(passwordPage.password().get());
					tiles.remove(passwordPage);
					sem.release();
				});
				passwordPage.onCancel((e) -> {
					tiles.remove(passwordPage);
					sem.release();
				});
			});
			sem.acquire();
		} catch (InterruptedException ie) {
			throw new IllegalStateException("Interrupted.", ie);
		} finally {
			sem.release();
		}
		return buf.length() == 0 ? null : buf.toString();
	}

	@Override
	protected Node createContent(Stage stage, JajaFXAppWindow<PushSFTPUIApp> window) {

		tiles = new Tiles<>(this, window);
		tiles.add(DropPage.class);
		tiles.getStyleClass().add("padded");
		return tiles;
	}

	public void notification(ToastType level, String title, String content) {
		notification(NotificationType.valueOf(level.name()), title, content);
	}

	public void notification(NotificationType level, String title, String content) {
		var dropPage = getTiles().getPage(DropPage.class);
		var notificationPane = dropPage.notificationPane;

		notificationPane.getStyleClass().removeAll("notification-danger", "notification-warning", "notification-info", "notification-success");
		switch(level) {
		case ERROR:
			FontIcon gr = FontIcon.of(FontAwesomeSolid.EXCLAMATION_CIRCLE);
			notificationPane.setGraphic(gr);
			notificationPane.getStyleClass().add("notification-danger");
			Toast.toast(ToastType.ERROR, title, content);
			break;
		case WARNING:
			gr = FontIcon.of(FontAwesomeSolid.EXCLAMATION_TRIANGLE);
			notificationPane.setGraphic(gr);
			notificationPane.getStyleClass().add("notification-warning");
			Toast.toast(ToastType.WARNING, title, content);
			break;
		case INFO:
			gr = FontIcon.of(FontAwesomeSolid.INFO_CIRCLE);
			notificationPane.setGraphic(gr);
			notificationPane.getStyleClass().add("notification-info");
			Toast.toast(ToastType.INFO, title, content);
			break;
		case SUCCESS:
			gr = FontIcon.of(FontAwesomeSolid.CHECK_CIRCLE);
			notificationPane.setGraphic(gr);
			notificationPane.getStyleClass().add("notification-success");
			Toast.toast(ToastType.INFO, title, content);
			break;
		default:
			notificationPane.setGraphic(null);
			Toast.toast(ToastType.NONE, title, content);
			break;
		}
		notificationPane.show(title + ". " + content);
	}
}