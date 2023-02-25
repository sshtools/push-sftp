package com.sshtools.pushsftp.jfx;

import java.nio.file.Paths;
import java.util.ResourceBundle;

import com.sshtools.jajafx.AbstractWizardPage;
import com.sshtools.jajafx.FXUtil;
import com.sshtools.jajafx.Phase;
import com.sshtools.jajafx.PrefBind;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;

public class OptionsPage extends AbstractWizardPage<PushSFTPUIApp> {

	final static ResourceBundle RESOURCES = ResourceBundle.getBundle(OptionsPage.class.getName());

	private PrefBind prefBind;

	@FXML
	TextField agentSocket;
	@FXML
	CheckBox verbose;
	@FXML
	CheckBox automaticUpdates;
	@FXML
	ComboBox<Phase> phase;

	private FileChooser agentSocketChooser;

	@Override
	protected void onConfigure() {
		
		phase.getItems().addAll(Phase.values());
		
		prefBind = new PrefBind(PushSFTPUIApp.PREFERENCES);
		prefBind.bind(verbose, automaticUpdates);
		prefBind.bind(agentSocket);
		prefBind.bind(Phase.class, phase);

		agentSocket.setPromptText(System.getenv("SSH_AUTH_SOCK"));
		
	}

	@Override
	public void shown() {
		getWizard().nextVisibleProperty().set(false);
	}

	@Override
	public void hidden() {
		getWizard().nextVisibleProperty().set(true);
	}

	@FXML
	private void browseAgentSocket() {
		if (agentSocketChooser == null) {
			agentSocketChooser = new FileChooser();
			agentSocketChooser.setInitialDirectory(Paths.get(FXUtil.textOrPrompt(agentSocket)).getParent().toFile());
		}
		agentSocketChooser.setTitle(RESOURCES.getString("agentSocket.choose.title"));
		var selectedDirectory = agentSocketChooser.showOpenDialog(getScene().getWindow());
		if (selectedDirectory != null) {
			agentSocket.setText(selectedDirectory.getAbsolutePath());
		}
	}

	@Override
	public void close() {
		prefBind.close();
	}

}
