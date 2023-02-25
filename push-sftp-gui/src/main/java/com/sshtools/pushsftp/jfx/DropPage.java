package com.sshtools.pushsftp.jfx;

import static com.sshtools.simjac.AttrBindBuilder.xboolean;
import static com.sshtools.simjac.AttrBindBuilder.xinteger;
import static com.sshtools.simjac.AttrBindBuilder.xstring;

import java.util.Optional;
import java.util.ResourceBundle;

import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

import com.sshtools.jajafx.AboutPage;
import com.sshtools.jajafx.AbstractWizardPage;
import com.sshtools.jajafx.SequinsProgress;
import com.sshtools.pushsftp.jfx.PushJob.PushJobBuilder;
import com.sshtools.pushsftp.jfx.Target.TargetBuilder;
import com.sshtools.simjac.ConfigurationStoreBuilder;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.DragEvent;
import javafx.scene.input.TransferMode;
import javafx.util.Duration;

public class DropPage extends AbstractWizardPage<PushSFTPUIApp> {

	final static ResourceBundle RESOURCES = ResourceBundle.getBundle(DropPage.class.getName());

	@FXML
	private FontIcon folderIcon;
	@FXML
	private FontIcon fileIcon;

	@FXML
	private Label text;

	@FXML
	private SequinsProgress progress;

	private Button addTarget;
	private Button about;
	private Button options;
	private SequentialTransition anim;
	private FileTransferService service;

	private boolean hovering;

	@Override
	protected void onConfigure() {
		service = getContext().getService();
		service.busyProperty().addListener((c, o, n) -> {
			if (n) {
				fileIcon.setOpacity(1);
				fileIcon.setVisible(true);
				anim.play();
			} else {
				anim.stop();
				fileIcon.setVisible(false);
			}
			updateFolderIcon();
		});

		addTarget = new Button(RESOURCES.getString("addTarget"));
		addTarget.setOnAction(e -> getWizard().popup(EditTargetPage.class));
//		addTarget.setGraphic(new FontIcon(FontAwesomeSolid.PLUS));
		addTarget.setGraphic(new FontIcon(FontAwesomeSolid.USER_COG));

		about = new Button(RESOURCES.getString("about"));
		about.setOnAction(e -> getWizard().popup(AboutPage.class));
		about.setGraphic(new FontIcon(FontAwesomeSolid.HANDS_HELPING));

		options = new Button(RESOURCES.getString("options"));
		options.setOnAction(e -> getWizard().popup(OptionsPage.class));
		options.setGraphic(new FontIcon(FontAwesomeSolid.COGS));

		var t1 = new TranslateTransition(Duration.seconds(3));
		t1.setFromX(0);
		t1.setToX(0);
		t1.setFromY(-128);
		t1.setToY(0);

		var f1 = new FadeTransition(Duration.seconds(3));
		f1.setFromValue(1.0);
		f1.setToValue(0);

		var tp = new ParallelTransition(t1, f1);

		var t2 = new TranslateTransition();
		t2.setToX(0);
		t2.setToY(-128);

		var f2 = new FadeTransition(Duration.seconds(0.25f));
		f2.setFromValue(0);
		f2.setToValue(1);

		var tr = new SequentialTransition(tp, t2, f2);
		tr.setInterpolator(Interpolator.EASE_BOTH);
		tr.setNode(fileIcon);
		tr.setCycleCount(Animation.INDEFINITE);
		anim = tr;
	}

	@Override
	public void shown() {
		getWizard().getAccessories().getChildren().addAll(addTarget, about, options);
		getWizard().nextVisibleProperty().set(false);

	}

	@Override
	public void hidden() {
		getWizard().getAccessories().getChildren().removeAll(addTarget, about, options);
		getWizard().nextVisibleProperty().set(true);
	}

	@FXML
	void dragEnter(DragEvent evt) {
		hovering = true;
		updateFolderIcon();
	}

	@FXML
	void dragOver(DragEvent evt) {
		if (evt.getDragboard().hasFiles()) {
			evt.acceptTransferModes(TransferMode.COPY);
		}
		evt.consume();
	}

	@FXML
	void dragExit(DragEvent evt) {
		hovering = false;
		updateFolderIcon();
	}

	@FXML
	void drop(DragEvent evt) {
		var db = evt.getDragboard();
		evt.setDropCompleted(db.hasFiles());
		evt.consume();

		if (db.hasFiles()) {

			var bldr = TargetBuilder.builder();

			ConfigurationStoreBuilder.builder().withApp(PushSFTPUI.class).withName("targets").withoutFailOnMissingFile()
					.withBinding(xstring("hostname", bldr::withHostname).build(),
							xstring("username", bldr::withUsername).build(),
							xstring("remoteFolder", bldr::withRemoteFolder).build(),
							xstring("privateKey", bldr::withIdentity).build(), xinteger("port", bldr::withPort).build(),
							xboolean("agentAuthentication", bldr::withAgent).build(),
							xboolean("defaultIdentities", bldr::withIdentities).build(),
							xboolean("passwordAuthentication", bldr::withPassword).build())
					.build().retrieve();

			var target = bldr.build();
			var agentSocket = PushSFTPUIApp.PREFERENCES.get("agentSocket", "");
			
			service.submit(PushJobBuilder.builder()
					.withVerbose(PushSFTPUIApp.PREFERENCES.getBoolean("verbose", false))
					.withAgentSocket(agentSocket.equals("")? Optional.empty() : Optional.of(agentSocket))
					.withProgress(progress.createProgress(RESOURCES.getString("progressMessage"), db.getFiles().size()))
					.withFiles(db.getFiles()).withTarget(target)
					.withPassphrasePrompt(getContext().createPassphrasePrompt(target))
					.withPassword(getContext().createPasswordPrompt(target))
					.build());
		}
	}
	
	private void updateFolderIcon() {
		if(hovering || service.busyProperty().get())
			folderIcon.setIconCode(FontAwesomeSolid.FOLDER_OPEN);
		else
			folderIcon.setIconCode(FontAwesomeSolid.FOLDER);
	}

}
