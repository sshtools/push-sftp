package com.sshtools.pushsftp.jfx;

import static com.sshtools.jajafx.FXUtil.intTextfieldValue;
import static com.sshtools.jajafx.FXUtil.makeIntegerTextField;

import java.util.ResourceBundle;

import com.sshtools.client.sftp.RemoteHash;
import com.sshtools.jajafx.AbstractTile;
import com.sshtools.pushsftp.jfx.Target.TargetBuilder;

import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.util.StringConverter;

public class AdvancedEditTargetPage extends AbstractTile<PushSFTPUIApp> {

	final static ResourceBundle RESOURCES = ResourceBundle.getBundle(AdvancedEditTargetPage.class.getName());

	@FXML
	TextField chunks;
	@FXML
	CheckBox agentAuthentication;
	@FXML
	CheckBox passwordAuthentication;
	@FXML
	CheckBox defaultIdentities;
	@FXML
	ComboBox<Mode> mode;
	@FXML
	ComboBox<RemoteHash> hash;
	@FXML
	CheckBox verifyIntegrity;
	@FXML
	CheckBox multiplex;
	@FXML
	CheckBox ignoreIntegrity;


	@Override
	public void shown() {
	}

	@Override
	public void hidden() {
	}

	public void save(TargetBuilder builder) {
		builder.
				withChunks(intTextfieldValue(chunks)).
				withPassword(passwordAuthentication.isSelected()).
				withIdentities(defaultIdentities.isSelected()).
				withMode(mode.getSelectionModel().getSelectedItem()).
				withMultiplex(multiplex.isSelected()).
				withAgent(agentAuthentication.isSelected()).
				withVerifyIntegrity(verifyIntegrity.isSelected()).
				withIgnoreIntegrity(ignoreIntegrity.isSelected());
	}

	@FXML
	private void back() {
		getTiles().remove(this);
	}

	@Override
	protected void onConfigure() {
		mode.getItems().addAll(Mode.values());
		mode.getSelectionModel().select(Mode.CHUNKED);
		hash.getItems().addAll(RemoteHash.values());
		hash.getSelectionModel().select(RemoteHash.sha512);
		makeIntegerTextField(1, 99, chunks);
		
		var standardMode = Bindings.or(Bindings.equal(mode.valueProperty(), Mode.SCP), Bindings.equal(mode.valueProperty(), Mode.SFTP));
		
		chunks.disableProperty().bind(standardMode);
		verifyIntegrity.disableProperty().bind(standardMode);
		ignoreIntegrity.disableProperty().bind(standardMode);
		hash.disableProperty().bind(standardMode);

		mode.setConverter(new StringConverter<Mode>() {
			
			@Override
			public String toString(Mode object) {
				return RESOURCES.getString("mode." + object.name());
			}
			
			@Override
			public Mode fromString(String string) {
				return null;
			}
		});
	}

	@Override
	public void close() {
	}

	public void setTarget(Target target) {
		chunks.setText(String.valueOf(target.chunks()));
		agentAuthentication.setSelected(target.agent());
		passwordAuthentication.setSelected(target.password());
		multiplex.setSelected(target.multiplex());
		defaultIdentities.setSelected(target.identities());
		mode.getSelectionModel().select(target.mode());
		verifyIntegrity.setSelected(target.verifyIntegrity());
		ignoreIntegrity.setSelected(target.ignoreIntegrity());
		hash.getSelectionModel().select(target.hash());
	}
}
