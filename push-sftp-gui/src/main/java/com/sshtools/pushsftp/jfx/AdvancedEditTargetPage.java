package com.sshtools.pushsftp.jfx;

import static com.sshtools.jajafx.FXUtil.intTextfieldValue;
import static com.sshtools.jajafx.FXUtil.makeIntegerTextField;

import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.Consumer;

import com.sshtools.client.sftp.RemoteHash;
import com.sshtools.jajafx.AbstractTile;
import com.sshtools.pushsftp.jfx.Target.TargetBuilder;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;

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
	CheckBox ignoreIntegrity;
	@FXML
	CheckBox preAllocate;
	@FXML
	CheckBox copyDataExtension;


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
				withAgent(agentAuthentication.isSelected()).
				withCopyDataExtension(copyDataExtension.isSelected()).
				withPreAllocate(preAllocate.isSelected()).
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
	}

	@Override
	public void close() {
	}

	public void setTarget(Target target) {
		chunks.setText(String.valueOf(target.chunks()));
		agentAuthentication.setSelected(target.agent());
		passwordAuthentication.setSelected(target.password());
		defaultIdentities.setSelected(target.identities());
		mode.getSelectionModel().select(target.mode());
		copyDataExtension.setSelected(target.copyDataExtension());
		preAllocate.setSelected(target.preAllocate());
		verifyIntegrity.setSelected(target.verifyIntegrity());
		ignoreIntegrity.setSelected(target.ignoreIntegrity());
		hash.getSelectionModel().select(target.hash());
	}
}
