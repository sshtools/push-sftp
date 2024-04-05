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

import com.sshtools.jajafx.FXUtil;
import com.sshtools.jajafx.PageTransition;
import com.sshtools.pushsftp.jfx.SshTarget.SshTargetBuilder;

import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;

public class EditSshTargetPage extends AbstractEditTargetPage<SshTarget> {

	final static ResourceBundle RESOURCES = ResourceBundle.getBundle(EditSshTargetPage.class.getName());

	@FXML
	TextField username;
	@FXML
	PasswordField unsafePassword;
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
	@FXML
	HBox unsafePasswordContainer;

	private AdvancedEditTargetPage advanced;

	@Override
	public void hidden() {
		advanced = null;
	}

	@FXML
	private void save() {
		var bldr = createTarget();
		if (advanced != null)
			advanced.save(bldr);
		onSave.accept(bldr.build());
		getTiles().remove(this);
	}

	private SshTargetBuilder createTarget() {
		return new SshTargetBuilder().
				fromTarget(target).
				withUsername(textOrPrompt(username)).
				withUnsafePassword(FXUtil.optionalText(unsafePassword.getText())).
				withHostname(textOrPrompt(hostname)).
				withPort(intTextfieldValue(port)).
				withDisplayName(optionalText(displayName)).
				withIdentityPath(optionalText(privateKey)).
				withRemoteFolderPath(optionalText(remoteFolder));
	}

	@FXML
	private void advanced() {
		advanced = getContext().getTiles().popup(AdvancedEditTargetPage.class, PageTransition.FROM_RIGHT);
		advanced.setTarget(target);
	}

	@Override
	protected void onEditConfigure() {
		username.setPromptText(System.getProperty("user.name"));
		makeIntegerTextField(0, 65535, port);
		unsafePasswordContainer.managedProperty().bind(unsafePasswordContainer.visibleProperty());
	}

	@FXML
	private void browseRemoteFolder() throws Exception {
		var browser = getContext().getTiles().popup(BrowsePage.class, PageTransition.FROM_RIGHT);
		browser.setTarget(createTarget().build(), FXUtil.optionalText(remoteFolder));
		browser.setOnSelect(path -> remoteFolder.setText(path.toString()));
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
	protected void onSetTarget(SshTarget target, Consumer<SshTarget> onSave, Optional<Consumer<SshTarget>> onDelete) {
		
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
		unsafePassword.setText(target.unsafePassword().orElse(""));
		unsafePasswordContainer.setVisible(!getContext().isKeyringAvailable());
		rebuildDisplayNamePrompt();
	}

	private void rebuildDisplayNamePrompt() {
		displayName.setPromptText(SshTarget.sshDisplayName(Integer.parseInt(FXUtil.textOrPrompt(port)), textOrPrompt(username), textOrPrompt(hostname), remoteFolder.getText()));
	}
}
