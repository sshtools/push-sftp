package com.sshtools.pushsftp.jfx;

import static javafx.application.Platform.runLater;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.ResourceBundle;

import com.sshtools.client.SshClient;
import com.sshtools.client.sftp.SftpClient;
import com.sshtools.jajafx.AbstractTile;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;

public class BrowsePage extends AbstractTile<PushSFTPUIApp> {

	final static ResourceBundle RESOURCES = ResourceBundle.getBundle(BrowsePage.class.getName());

	@FXML
	TreeView<Path> browser;

	private SshClient connection;
	private SftpClient sftp;

	@Override
	protected void onConfigure() {

	}

	@FXML
	private void back(ActionEvent evt) {
		getTiles().remove(this);

	}

	@Override
	public void close() {
		if(connection != null) {
			try {
				connection.close();
			} catch (IOException e) {
			}
		}
	}

	void refreshView() {
	}

	void setTarget(Target build) throws Exception {

		var context = getContext();
		var prefs = context.getContainer().getAppPreferences();
		var agentSocket = prefs.get("agentSocket", "");
		var job = SshConnectionJob.forConnection().
				withTarget(build).
				withVerbose(prefs.getBoolean("verbose", false)).
				withAgentSocket(agentSocket.equals("") ? Optional.empty() : Optional.of(agentSocket)).
				withPassword(context.createPasswordPrompt(build)).
				build();

		context.getContainer().getScheduler().execute(() -> {
			try {
				connection = job.call();
				sftp = new SftpClient(connection);

				var rootPath = Path.of(sftp.pwd());
				var rootItem = new TreeItem<>(rootPath);
				runLater(() -> browser.setRoot(rootItem));
				var it = sftp.lsIterator();
				while (it.hasNext()) {
					var file = it.next();
					if(file.isDirectory() && !file.getFilename().startsWith("."))
						runLater(() -> rootItem.getChildren().add(new TreeItem<>(Path.of(file.getFilename()))));
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

		});

	}

}
