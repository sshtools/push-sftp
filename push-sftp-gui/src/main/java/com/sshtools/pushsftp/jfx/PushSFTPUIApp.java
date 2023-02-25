package com.sshtools.pushsftp.jfx;

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
import com.sshtools.jajafx.UpdatePage;
import com.sshtools.jajafx.Wizard;

import javafx.application.Platform;
import javafx.scene.Node;

public class PushSFTPUIApp extends JajaFXApp<PushSFTPUI> {

	final static ResourceBundle RESOURCES = ResourceBundle.getBundle(PushSFTPUI.class.getName());
	final static Preferences PREFERENCES = Preferences.userNodeForPackage(PushSFTPUI.class);
	
	private FileTransferService service;
	private Wizard<PushSFTPUIApp> wiz;
	private Keyring keyring;

	public PushSFTPUIApp() {
		super(PushSFTPUIApp.class.getResource("icon.png"), (PushSFTPUI) PushSFTPUI.getInstance());
		service = new FileTransferService();
		try {
			keyring = Keyring.create();
		} catch (BackendNotSupportedException e) {
			e.printStackTrace();
		}
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
		if(!(wiz.getCurrentPage() instanceof AboutPage) && !(wiz.getCurrentPage() instanceof UpdatePage)
				&& !service.busyProperty().get()) {
			wiz.popup(UpdatePage.class);
		}
	}

	public PassphrasePrompt createPassphrasePrompt(Target target) {
		var save = new AtomicBoolean();
		return new PassphrasePrompt() {
			@Override
			public String getPasshrase(String keyinfo) {
				try {
					if(keyring == null)
						throw new PasswordAccessException("No keyring.");
					return keyring.getPassword(keyinfo, target.username());
				}
				catch(PasswordAccessException pae) {
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
		var serviceName = target.hostname() + ":" + target.port() + "/" + target.remoteFolder().map(Path::toString).orElse("");
		return new PasswordPrompt() {
			
			@Override
			public String get() {
				try {
					if(keyring == null)
						throw new PasswordAccessException("No keyring.");
					return keyring.getPassword(serviceName, target.username());
				}
				catch(PasswordAccessException pae) {
					return password(save, target.username(), RESOURCES.getString("passwordDialog.title"));
				}
			}

			@Override
			public void completed(boolean success, String value, ClientAuthenticator authenticator) {
				if(keyring != null) {
					try {
						if(success) {
							if(save.get())
								keyring.setPassword(serviceName, target.username(), value);
						}
						else {
							keyring.deletePassword(serviceName, target.username());
						}
					}
					catch(PasswordAccessException pae) {
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
				var passwordPage = (PasswordPage<PushSFTPUIApp>) wiz.popup(PasswordPage.class);
				var txt = MessageFormat.format(fmt, args);
				passwordPage.titleText().set(txt);
				passwordPage.textText()
						.set(MessageFormat.format(RESOURCES.getString("passwordDialog.prompt"), username));
				passwordPage.setConfirm((e) -> {
					save.set(passwordPage.isSave());
					buf.append(passwordPage.password().get());
					wiz.remove(passwordPage);
					sem.release();
				});
				passwordPage.setCancel((e) -> {
					wiz.remove(passwordPage);
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
		wiz = new Wizard<>(this);
		wiz.add(DropPage.class);
		return wiz;
	}
}