package com.sshtools.pushsftp.jfx;

import static com.sshtools.jajafx.FXUtil.intTextfieldValue;
import static com.sshtools.jajafx.FXUtil.makeIntegerTextField;
import static com.sshtools.simjac.AttrBindBuilder.xboolean;
import static com.sshtools.simjac.AttrBindBuilder.xinteger;
import static com.sshtools.simjac.AttrBindBuilder.xstring;

import java.nio.file.Paths;
import java.util.ResourceBundle;

import com.sshtools.jajafx.AbstractWizardPage;
import com.sshtools.simjac.ConfigurationStore;
import com.sshtools.simjac.ConfigurationStoreBuilder;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;

public class EditTargetPage extends AbstractWizardPage<PushSFTPUIApp> {

	final static ResourceBundle RESOURCES = ResourceBundle.getBundle(EditTargetPage.class.getName());

	@FXML
	TextField username;
	@FXML
	TextField hostname;
	@FXML
	TextField port;
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

	private FileChooser keyChooser;
	private ConfigurationStore store;

	@Override
	public void shown() {
		store.retrieve();
		getWizard().toolsVisibleProperty().set(false);
	}

	@Override
	public void hidden() {
		getWizard().toolsVisibleProperty().set(true);
	}
	
	@FXML
	private void save() {
		store.store();
		getWizard().remove(this);
	}
	
	@FXML
	private void cancel() {
		getWizard().remove(this);
	}

	@Override
	protected void onConfigure() {
		username.setPromptText(System.getProperty("user.name"));

		makeIntegerTextField(0, 65535, port);
		store = ConfigurationStoreBuilder.builder().
				withApp(PushSFTPUI.class).
				withName("targets").
				withoutFailOnMissingFile().
				withBinding(
					xstring("hostname", hostname::setText, hostname::getText).build(),
					xstring("username", username::setText, username::getText).build(),
					xstring("remoteFolder", remoteFolder::setText, remoteFolder::getText).build(),
					xstring("privateKey", privateKey::setText, privateKey::getText).build(),
					xinteger("port", v-> port.setText(String.valueOf(v)), ()-> intTextfieldValue(port)).build(),
					xboolean("agentAuthentication", agentAuthentication::setSelected, agentAuthentication::isSelected).build(),
					xboolean("defaultIdentities", defaultIdentities::setSelected, defaultIdentities::isSelected).build(),
					xboolean("passwordAuthentication", passwordAuthentication::setSelected, passwordAuthentication::isSelected).build()
				).build();
		store.retrieve();
	}

	@FXML
	private void browsePrivateKey() {
		if (keyChooser == null) {
			keyChooser = new FileChooser();
			keyChooser.setInitialDirectory(Paths.get(System.getProperty("user.home"), ".ssh").toFile());
		}
		keyChooser.setTitle(RESOURCES.getString("privateKey.choose.title"));
		var selectedDirectory = keyChooser.showOpenDialog(getScene().getWindow());
		if (selectedDirectory != null) {
			privateKey.setText(selectedDirectory.getAbsolutePath());
		}
	}

	@Override
	public void close() {
		store.close();
	}

}
