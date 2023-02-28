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
import com.sshtools.pushsftp.jfx.Target.TargetBuilder;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;

public class EditTargetPage extends AbstractTile<PushSFTPUIApp> {

	final static ResourceBundle RESOURCES = ResourceBundle.getBundle(EditTargetPage.class.getName());

	@FXML
	TextField username;
	@FXML
	TextField hostname;
	@FXML
	TextField port;
	@FXML
	TextField chunks;
	@FXML
	TextField privateKey;
	@FXML
	TextField remoteFolder;
	@FXML
	CheckBox agentAuthentication;
	@FXML
	CheckBox passwordAuthentication;
	@FXML
	CheckBox defaultIdentities;
	@FXML
	ComboBox<Mode> mode;

	private Consumer<Target> onSave;
	private Optional<Runnable> onDelete;

	@Override
	public void shown() {
	}

	@Override
	public void hidden() {
	}

	@FXML
	private void save() {
		onSave.accept(TargetBuilder.builder().
				withUsername(textOrPrompt(username)).
				withHostname(textOrPrompt(hostname)).
				withPort(intTextfieldValue(port)).
				withChunks(intTextfieldValue(chunks)).
				withIdentityPath(optionalText(privateKey)).
				withRemoteFolderPath(optionalText(remoteFolder)).
				withPassword(passwordAuthentication.isSelected()).
				withIdentities(defaultIdentities.isSelected()).
				withMode(mode.getSelectionModel().getSelectedItem()).
				withAgent(agentAuthentication.isSelected()).
				build());
		getTiles().remove(this);
	}

	@FXML
	private void cancel() {
		getTiles().remove(this);
	}

	@FXML
	private void delete() {
		onDelete.ifPresent(r -> r.run());
		getTiles().remove(this);
	}

	@Override
	protected void onConfigure() {
		username.setPromptText(System.getProperty("user.name"));
		mode.getItems().addAll(Mode.values());
		mode.getSelectionModel().select(Mode.CHUNKED);
		makeIntegerTextField(0, 65535, port);
		makeIntegerTextField(1, 99, chunks);
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
		this.onDelete = onDelete.map(r -> new Runnable() {
			@Override
			public void run() {
				r.accept(target);
			}
		});
		
		username.setText(target.username());
		hostname.setText(target.hostname());
		port.setText(String.valueOf(target.port()));
		chunks.setText(String.valueOf(target.chunks()));
		privateKey.setText(target.identity().map(Path::toString).orElse(""));
		remoteFolder.setText(target.remoteFolder().map(Path::toString).orElse(""));
		agentAuthentication.setSelected(target.agent());
		passwordAuthentication.setSelected(target.password());
		defaultIdentities.setSelected(target.identities());
		mode.getSelectionModel().select(target.mode());
	}
}
