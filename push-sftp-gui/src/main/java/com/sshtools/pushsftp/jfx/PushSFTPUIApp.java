package com.sshtools.pushsftp.jfx;

import static com.sshtools.jajafx.FXUtil.maybeQueue;

import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ResourceBundle;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.prefs.Preferences;

import com.github.javakeyring.BackendNotSupportedException;
import com.github.javakeyring.Keyring;
import com.github.javakeyring.PasswordAccessException;
import com.sshtools.client.ClientAuthenticator;
import com.sshtools.client.PassphrasePrompt;
import com.sshtools.client.PasswordAuthenticator.PasswordPrompt;
import com.sshtools.jajafx.AboutPage;
import com.sshtools.jajafx.JajaFXApp;
import com.sshtools.jajafx.PasswordPage;
import com.sshtools.jajafx.Tiles;
import com.sshtools.jajafx.UpdatePage;

import javafx.application.Platform;
import javafx.scene.Node;

public class PushSFTPUIApp extends JajaFXApp<PushSFTPUI> {

	final static ResourceBundle RESOURCES = ResourceBundle.getBundle(PushSFTPUI.class.getName());
	final static Preferences PREFERENCES = Preferences.userNodeForPackage(PushSFTPUI.class);

	private FileTransferService service;
	private Tiles<PushSFTPUIApp> tiles;
	private Keyring keyring;

	public PushSFTPUIApp() {
		super(PushSFTPUIApp.class.getResource("icon.png"), RESOURCES.getString("title"), (PushSFTPUI) PushSFTPUI.getInstance());
		service = new FileTransferService();
		try {
			keyring = Keyring.create();
		} catch (BackendNotSupportedException e) {
			e.printStackTrace();
		}
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
		return keyring != null;
	}

	@Override
	protected void needUpdate() {
		maybeQueue(() -> {
			if (!(tiles.getCurrentPage() instanceof AboutPage) && !(tiles.getCurrentPage() instanceof UpdatePage)
					&& !service.busyProperty().get()) {
				tiles.popup(UpdatePage.class);
			}
		});
	}

	public PassphrasePrompt createPassphrasePrompt(Target target) {
		var save = new AtomicBoolean();
		return new PassphrasePrompt() {
			@Override
			public String getPasshrase(String keyinfo) {
				try {
					if (keyring == null)
						throw new PasswordAccessException("No keyring.");
					return keyring.getPassword(keyinfo, target.username());
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
		var serviceName = target.hostname() + ":" + target.port() + "/"
				+ target.remoteFolder().map(Path::toString).orElse("");
		return new PasswordPrompt() {

			@Override
			public String get() {
				try {
					if (keyring == null)
						throw new PasswordAccessException("No keyring.");
					return keyring.getPassword(serviceName, target.username());
				} catch (PasswordAccessException pae) {
					return password(save, target.username(), RESOURCES.getString("passwordDialog.title"));
				}
			}

			@Override
			public void completed(boolean success, String value, ClientAuthenticator authenticator) {
				if (keyring != null) {
					try {
						if (success) {
							if (save.get())
								keyring.setPassword(serviceName, target.username(), value);
						} else {
							keyring.deletePassword(serviceName, target.username());
						}
					} catch (PasswordAccessException pae) {
					}
				}
			}
		};
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
				passwordPage.setConfirm((e) -> {
					save.set(passwordPage.isSave());
					buf.append(passwordPage.password().get());
					tiles.remove(passwordPage);
					sem.release();
				});
				passwordPage.setCancel((e) -> {
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
	protected Node createContent() {
		tiles = new Tiles<>(this);
		tiles.add(DropPage.class);
		tiles.getStyleClass().add("padded");
		return tiles;
	}
}