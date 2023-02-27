package com.sshtools.pushsftp.jfx;

import static com.sshtools.jajafx.FXUtil.chooseFileAndRememeber;
import static com.sshtools.jajafx.FXUtil.intTextfieldValue;
import static com.sshtools.jajafx.FXUtil.makeIntegerTextField;
import static com.sshtools.simjac.AttrBindBuilder.xboolean;
import static com.sshtools.simjac.AttrBindBuilder.xinteger;
import static com.sshtools.simjac.AttrBindBuilder.xstring;

import java.nio.file.Paths;
import java.util.ResourceBundle;

import com.sshtools.jajafx.AbstractTile;
import com.sshtools.simjac.ConfigurationStore;
import com.sshtools.simjac.ConfigurationStoreBuilder;

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
	ComboBox<TransferMode> mode;

	private ConfigurationStore store;

	@Override
	public void shown() {
		store.retrieve();
	}

	@Override
	public void hidden() {
	}

	@FXML
	private void save() {
		store.store();
		getTiles().remove(this);
	}

	@FXML
	private void cancel() {
		getTiles().remove(this);
	}

	@Override
	protected void onConfigure() {
		username.setPromptText(System.getProperty("user.name"));
		mode.getItems().addAll(TransferMode.values());

		makeIntegerTextField(0, 65535, port);
		store = ConfigurationStoreBuilder.builder().withApp(PushSFTPUI.class).withName("targets")
				.withoutFailOnMissingFile()
				.withBinding(xstring("hostname", hostname::setText, hostname::getText).build(),
						xstring("username", username::setText, username::getText).build(),
						xstring("remoteFolder", remoteFolder::setText, remoteFolder::getText).build(),
						xstring("privateKey", privateKey::setText, privateKey::getText).build(),
						xinteger("port", v -> port.setText(String.valueOf(v)), () -> intTextfieldValue(port)).build(),
						xboolean("agentAuthentication", agentAuthentication::setSelected,
								agentAuthentication::isSelected).build(),
						xboolean("defaultIdentities", defaultIdentities::setSelected, defaultIdentities::isSelected)
								.build(),
						xboolean("passwordAuthentication", passwordAuthentication::setSelected,
								passwordAuthentication::isSelected).build())
				.build();
		store.retrieve();
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
		store.close();
	}

}
