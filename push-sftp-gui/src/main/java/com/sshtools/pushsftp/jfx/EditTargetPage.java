package com.sshtools.pushsftp.jfx;

import static com.sshtools.jajafx.FXUtil.chooseFileAndRememeber;
import static com.sshtools.jajafx.FXUtil.intTextfieldValue;
import static com.sshtools.jajafx.FXUtil.makeIntegerTextField;
import static com.sshtools.jajafx.FXUtil.optionalText;
import static com.sshtools.jajafx.FXUtil.textOrPrompt;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.Consumer;

import com.sshtools.jajafx.AbstractTile;
import com.sshtools.jajafx.FXUtil;
import com.sshtools.jajafx.PageTransition;
import com.sshtools.pushsftp.jfx.Target.TargetBuilder;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;

public class EditTargetPage extends AbstractTile<PushSFTPUIApp> {

	final static ResourceBundle RESOURCES = ResourceBundle.getBundle(EditTargetPage.class.getName());

	@FXML
	TextField username;
	@FXML
	TextField hostname;
	@FXML
	TextField displayName;
	@FXML
	TextField port;
	@FXML
	TextField privateKey;
	@FXML
	TextField remoteFolder;

	private Consumer<Target> onSave;
	private Optional<Runnable> onDelete;

	private Target target;

	private AdvancedEditTargetPage advanced;

	@Override
	public void shown() {
	}

	@Override
	public void hidden() {
		advanced = null;
	}

	@FXML
	private void save() {
		var bldr = TargetBuilder.builder().withUsername(textOrPrompt(username)).withHostname(textOrPrompt(hostname))
				.withPort(intTextfieldValue(port)).withDisplayName(optionalText(displayName))
				.withIdentityPath(optionalText(privateKey)).withRemoteFolderPath(optionalText(remoteFolder));
		if (advanced != null)
			advanced.save(bldr);
		onSave.accept(bldr.build());
		getTiles().remove(this);
	}

	@FXML
	private void cancel() {
		getTiles().remove(this);
	}

	@FXML
	private void advanced() {
		advanced = getContext().getTiles().popup(AdvancedEditTargetPage.class, PageTransition.FROM_RIGHT);
		advanced.setTarget(target);
	}

	@FXML
	private void delete() {
		onDelete.ifPresent(r -> r.run());
		getTiles().remove(this);
	}

	@Override
	protected void onConfigure() {
		username.setPromptText(System.getProperty("user.name"));
		makeIntegerTextField(0, 65535, port);
	}

	@FXML
	private void browsePrivateKey() {
		var keyChooser = new FileChooser();
		keyChooser.setTitle(RESOURCES.getString("privateKey.choose.title"));

		chooseFileAndRememeber(getContext().getContainer().getAppPreferences(), keyChooser,
				Paths.get(System.getProperty("user.home"), ".ssh"), "privateKey", getScene().getWindow())
				.ifPresent(f -> privateKey.setText(f.getAbsolutePath()));
	}

	@Override
	public void close() {
	}

	public void setTarget(Target target, Consumer<Target> onSave, Optional<Consumer<Target>> onDelete) {
		this.onSave = onSave;
		this.target = target;
		this.onDelete = onDelete.map(r -> new Runnable() {
			@Override
			public void run() {
				r.accept(target);
			}
		});
		displayName.setText(target.displayName().orElse(""));
		username.setText(target.username());
		username.textProperty().addListener((c, o, n) -> rebuildDisplayNamePrompt());
		hostname.setText(target.hostname());
		hostname.textProperty().addListener((c, o, n) -> rebuildDisplayNamePrompt());
		port.setText(String.valueOf(target.port()));
		port.textProperty().addListener((c, o, n) -> rebuildDisplayNamePrompt());
		privateKey.setText(target.identity().map(Path::toString).orElse(""));
		remoteFolder.setText(target.remoteFolder().map(Path::toString).orElse(""));
		remoteFolder.textProperty().addListener((c, o, n) -> rebuildDisplayNamePrompt());
	}

	private void rebuildDisplayNamePrompt() {
		displayName
				.setPromptText(textOrPrompt(username) + "@" + textOrPrompt(hostname) + ":" + FXUtil.textOrPrompt(port)
						+ ((remoteFolder.getText().equals("")) ? "/~"
								: (remoteFolder.getText().startsWith("/") ? remoteFolder.getText()
										: "/" + remoteFolder.getText())));
	}
}
