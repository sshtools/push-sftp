package com.sshtools.pushsftp.jfx;

import static javafx.application.Platform.runLater;

import java.io.IOException;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.Consumer;

import org.controlsfx.control.NotificationPane;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sshtools.client.SshClient;
import com.sshtools.client.sftp.SftpClient;
import com.sshtools.client.sftp.SftpClient.SftpClientBuilder;
import com.sshtools.jajafx.AbstractTile;

import javafx.beans.binding.Bindings;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;

public class BrowsePage extends AbstractTile<PushSFTPUIApp> {
	final static Logger LOG = LoggerFactory.getLogger(BrowsePage.class);

	final static ResourceBundle RESOURCES = ResourceBundle.getBundle(BrowsePage.class.getName());

	@FXML
	TreeView<Path> browser;
	
	@FXML
	private Hyperlink select;
	
	@FXML
	private BorderPane browseTop;

	private SshClient connection;
	private SftpClient sftp;
	private Consumer<Path> onSelect;
	private NotificationPane notificationPane;

	@Override
	protected void onConfigure() {
		select.disableProperty().bind(Bindings.isEmpty(browser.getSelectionModel().getSelectedItems()));
		browser.setOnMouseClicked(evt -> {
			if(evt.getClickCount() == 2) {
				select(null);
			}
		});
		
		var dropTopParent = browseTop.getParent();
		notificationPane = new NotificationPane(browseTop);
		((AnchorPane)dropTopParent).getChildren().setAll(notificationPane);
		AnchorPane.setBottomAnchor(notificationPane, 0d);
		AnchorPane.setTopAnchor(notificationPane, 0d);
		AnchorPane.setLeftAnchor(notificationPane, 0d);
		AnchorPane.setRightAnchor(notificationPane, 0d);
		
		browser.setCellFactory(param -> new TreeCell<Path>() {
			@Override
			public void updateItem(Path item, boolean empty) {
				super.updateItem(item, empty);
				if (!empty) {
					setText(item.getFileName().toString());
				}
			}
		});
	}

	@FXML
	private void back(ActionEvent evt) {
		getTiles().remove(this);

	}

	@FXML
	private void select(ActionEvent evt) {
		onSelect.accept(browser.getSelectionModel().getSelectedItem().getValue());
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

	void setTarget(Target target) throws Exception {

		var context = getContext();
		var prefs = context.getContainer().getAppPreferences();
		var agentSocket = prefs.get("agentSocket", "");
		var job = SshConnectionJob.forConnection().
				withTarget(target).
				withVerbose(prefs.getBoolean("verbose", false)).
				withAgentSocket(agentSocket.equals("") ? Optional.empty() : Optional.of(agentSocket)).
				withPassphrasePrompt(context.createPassphrasePrompt(target)).
				withPassword(context.createPasswordPrompt(target)).
				build();

		context.getContainer().getScheduler().execute(() -> {
			try {
				connection = job.call();
				notificationPane.hide();
				sftp = SftpClientBuilder.create().withClient(connection).build();

				var rootPath = Path.of(sftp.pwd());
				var rootItem = new TreeItem<>(rootPath);
				runLater(() -> browser.setRoot(rootItem));
				var it = sftp.lsIterator();
				while (it.hasNext()) {
					var file = it.next();
					if(file.attributes().isDirectory() && !file.getFilename().startsWith("."))
						runLater(() -> rootItem.getChildren().add(new TreeItem<>(rootPath.resolve(file.getFilename()))));
				}
			} catch (Exception e) {
				LOG.error("Failed to browse.", e);
				notify(FontIcon.of(FontAwesomeSolid.EXCLAMATION_CIRCLE), "notification-danger", "failed", target.bestDisplayName(), e.getMessage());
			}

		});
		notify(FontIcon.of(FontAwesomeSolid.INFO_CIRCLE), "notification-info", "connecting", target.bestDisplayName());

	}


	public void setOnSelect(Consumer<Path> onSelect) {
		this.onSelect = onSelect;
	}
	
	private void notify(Node graphic, String style, String key, Object... args) {
		notificationPane.getStyleClass().removeAll("notification-danger", "notification-warning", "notification-info", "notification-success");
		notificationPane.setGraphic(graphic);
		notificationPane.getStyleClass().add(style);
		notificationPane.show(MessageFormat.format(RESOURCES.getString(key), args));
	}

}
