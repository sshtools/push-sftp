package com.sshtools.pushsftp.jfx;

import static javafx.application.Platform.runLater;

import java.io.IOException;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;

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
			if (evt.getClickCount() == 2) {
				select(null);
			}
		});

		var dropTopParent = browseTop.getParent();
		notificationPane = new NotificationPane(browseTop);
		((AnchorPane) dropTopParent).getChildren().setAll(notificationPane);
		AnchorPane.setBottomAnchor(notificationPane, 0d);
		AnchorPane.setTopAnchor(notificationPane, 0d);
		AnchorPane.setLeftAnchor(notificationPane, 0d);
		AnchorPane.setRightAnchor(notificationPane, 0d);

		browser.setCellFactory(param -> new TreeCell<Path>() {
			@Override
			public void updateItem(Path item, boolean empty) {
				super.updateItem(item, empty);
				if (!empty) {
					setText(item.getFileName() == null ? "/" : item.getFileName().toString());
					var icon = FontIcon.of(FontAwesomeSolid.FOLDER);
					icon.getStyleClass().add("icon-accent");
					setGraphic(icon);
				}
				else {
					setText(null);
					setGraphic(null);
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
		if (connection != null) {
			try {
				connection.close();
			} catch (IOException e) {
			}
		}
	}

	void refreshView() {
	}

	void setTarget(SshTarget target, Optional<String> originalPath) throws Exception {

		var context = getContext();
		var prefs = context.getContainer().getAppPreferences();
		var agentSocket = prefs.get("agentSocket", "");
		var job = SshConnectionJob.forConnection()
				.withTarget(target)
				.withHostKeyVerification(context.createHostKeyVerificationPrompt())
				.withVerbose(prefs.getBoolean("verbose", false))
				.withAgentSocket(agentSocket.equals("") ? Optional.empty() : Optional.of(agentSocket))
				.withPassphrasePrompt(context.createPassphrasePrompt(target))
				.withPassword(context.createPasswordPrompt(target)).build();

		context.getContainer().getScheduler().execute(() -> {
			try {
				connection = job.call();
				sftp = SftpClientBuilder.create().withClient(connection).build();
				var rootPath = Path.of("/");
				var homePath = Path.of(originalPath.orElse(sftp.getDefaultDirectory()));
				var rootItem = new FileTreeItem(rootPath, target);
				runLater(() -> {
					browser.setRoot(rootItem);
					notificationPane.hide();
				});

				var item = rootItem;
				FileTreeItem child = null;
				for (int i = 0; i < homePath.getNameCount(); i++) {
					item.load();
					var fItem = item;
					runLater(() -> fItem.setExpanded(true));
					var name = homePath.getName(i);
					child = item.getChildForPath(name);
					if (child != null) {
						item = child;
					} else {
						break;
					}
				}

				var fItem = item;
				runLater(() -> {
					browser.getSelectionModel().select(fItem);
					browser.scrollTo(browser.getSelectionModel().getSelectedIndex());
				});
			} catch (Exception e) {
				LOG.error("Failed to browse.", e);
				runLater(() -> message(FontIcon.of(FontAwesomeSolid.EXCLAMATION_CIRCLE), "notification-danger", "failed",
						target.bestDisplayName(), e.getMessage()));
			}

		});
		message(FontIcon.of(FontAwesomeSolid.INFO_CIRCLE), "notification-info", "connecting", target.bestDisplayName());

	}

	public void setOnSelect(Consumer<Path> onSelect) {
		this.onSelect = onSelect;
	}

	private void message(Node graphic, String style, String key, Object... args) {
		notificationPane.getStyleClass().removeAll("notification-danger", "notification-warning", "notification-info",
				"notification-success");
		notificationPane.setGraphic(graphic);
		var msg = MessageFormat.format(RESOURCES.getString(key), args);
		notificationPane.setTooltip(new Tooltip(msg));
		notificationPane.getStyleClass().add(style);
		notificationPane.show(msg);
	}

	class FileTreeItem extends TreeItem<Path> {

		boolean loaded;
		boolean loading;
		Path path;
		SshTarget target;
		List<FileTreeItem> newItems = Collections.emptyList();

		public FileTreeItem(Path path, SshTarget target) {
			super(path);

			this.path = path;
			this.target = target;

			var context = getContext();

			expandedProperty().addListener((c, o, n) -> {
				if (n && !loaded && !loading) {
					context.getContainer().getScheduler().execute(() -> {
						load();
					});
				}
			});
		}

		void load() {
			try {
				loading = true;
				var it = sftp.lsIterator(path.toString());
				newItems = new ArrayList<FileTreeItem>();
				while (it.hasNext()) {
					var file = it.next();
					if (file.attributes().isDirectory() && !file.getFilename().startsWith("."))
						newItems.add(new FileTreeItem(path.resolve(file.getFilename()), target));
				}
				runLater(() -> getChildren().setAll(newItems));
				loaded = true;
			} catch (Exception e) {
				LOG.error("Failed to expand folder.", e);
				runLater(() -> message(FontIcon.of(FontAwesomeSolid.EXCLAMATION_TRIANGLE), "notification-warning", "failedToLoad",
						path, e.getMessage()));
			} finally {
				loading = false;
			}
		}

		FileTreeItem getChildForPath(Path name) {
			for (var c : newItems) {
				var item = (FileTreeItem) c;
				if (item.path.getFileName().equals(name)) {
					return item;
				}
			}
			return null;
		}

		@Override
		public boolean isLeaf() {
			return loaded && super.isLeaf();
		}

	}
}
