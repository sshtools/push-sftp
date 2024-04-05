package com.sshtools.pushsftp.jfx;

import static com.sshtools.jajafx.FXUtil.load;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sshtools.jajafx.FXUtil;
import com.sshtools.jajafx.PageTransition;
import com.sshtools.pushsftp.jfx.PushSFTPUIApp.NotificationType;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.TranslateTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.input.DragEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.util.Duration;

public class DropTarget extends StackPane implements Initializable {
	final static Logger LOG = LoggerFactory.getLogger(DropTarget.class);

	@FXML
	private FontIcon folderIcon;
	@FXML
	private FontIcon fileIcon;
	@FXML
	private Label folderText;
	@FXML
	private Label displayName;

	private boolean dragHovering;
	private boolean mouseHovering;
	private SequentialTransition anim;
	private FileTransferService service;
	private FadeTransition fade;
	private ResourceBundle resources;
	private PushSFTPUIApp context;
	private Target target;

	public DropTarget() {
		load(this);
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		this.resources = resources;
	}

	public Target getTarget() {
		return target;
	}
	
	public DropTarget setup(Target target, PushSFTPUIApp context) {
		this.context = context;
		this.target = target;


		folderText.setOpacity(0);
		displayName.setText(target.displayName().orElse(target.getDefaultDisplayName()));
		
		service = context.getService();
		service.busyProperty().addListener((c, o, n) -> {
			if (!fileIcon.isVisible() && n && service.isActive(target)) {
				fileIcon.setOpacity(1);
				fileIcon.setVisible(true);
				anim.play();
			} else if(fileIcon.isVisible() && !service.isActive(target)) {
				anim.stop();
				fileIcon.setVisible(false);
			}
			updateFolderIcon();
		});

		var t1 = new TranslateTransition(Duration.seconds(3));
		t1.setFromX(0);
		t1.setToX(0);
		t1.setFromY(-128);
		t1.setToY(0);

		var f1 = new FadeTransition(Duration.seconds(3));
		f1.setFromValue(1.0);
		f1.setToValue(0);
		
//		var c1 = new Transition() {
//
//			{
//				setCycleDuration(Duration.seconds(3));
//			}
//
//			protected void interpolate(double frac) {
//				Bounds boundsInLocal = fileIcon.getBoundsInLocal();
//				fileIcon.setClip(new Rectangle(boundsInLocal.getMinX(),boundsInLocal.getMinY(),boundsInLocal.getWidth(),boundsInLocal.getHeight() * frac));
//			}
//
//		};

		var tp = new ParallelTransition(t1, f1/* , c1 */);

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

		if(service.busyProperty().get()) {
			fileIcon.setOpacity(1);
			fileIcon.setVisible(true);
			anim.play();
			updateFolderIcon();
		}
		
		return this;
	}

	@FXML
	void click(MouseEvent evt) {
//		var flinger = (Flinger)getParent().getParent();
//		if(flinger.isWasDragged()) {
//			return;
//		}
		var keyChooser = new FileChooser();
		keyChooser.setTitle(resources.getString("drop.choose.title"));
		FXUtil.chooseFileAndRememeber(context.getContainer().getAppPreferences(), keyChooser,
				Paths.get(System.getProperty("user.home"), "Desktop"), "dropFile", getScene().getWindow())
				.ifPresent(f -> drop(Arrays.asList(f.toPath())));
	}

	@FXML
	void dragEnter(DragEvent evt) {
		dragHovering = true;
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
	void mouseEnter(MouseEvent evt) {
		mouseHovering = true;
		updateFolderIcon();
	}

	@FXML
	void dragExit(DragEvent evt) {
		dragHovering = false;
		updateFolderIcon();
	}

	@FXML
	void mouseExit(MouseEvent evt) {
		mouseHovering = false;
		updateFolderIcon();
	}

	@FXML
	void drop(DragEvent evt) {
		var db = evt.getDragboard();
		evt.setDropCompleted(db.hasFiles());
		evt.consume();

		if (db.hasFiles()) {
			drop(db.getFiles().stream().map(File::toPath).collect(Collectors.toList()));
		}
	}

	@FXML
	void edit(ActionEvent evt) {
		if(target instanceof SshTarget ssht) {
			edit(ssht, EditSshTargetPage.class);
		}
		else if(target instanceof HttpTarget httpt) {
			edit(httpt, EditHttpTargetPage.class);
		}
		else {
			throw new UnsupportedOperationException();
		}
	}
	
	private <TARG extends Target> void edit(TARG target, Class<? extends AbstractEditTargetPage<TARG>> clazz) {
		var targets = context.getService().getTargets();
		var pg = context.getTiles().popup(clazz, PageTransition.FROM_RIGHT);		
		pg.setTarget(target, (newTarget) -> {
			var idx = targets.indexOf(target);
			targets.set(idx, newTarget);
		}, Optional.of((t) -> {
			targets.remove(t);
		}));
	}

	private void drop(List<Path> files) {
		try {
			service.drop(target, files);
		} catch (Exception e) {
			LOG.error("Failed to drop files.", e);
			context.notification(NotificationType.ERROR, DropPage.RESOURCES.getString("error.failedToDrop"), e.getMessage());
		}
	}
	
	private void showFolderText(boolean show) {
		if(fade != null) {
			fade.stop();
			fade.getOnFinished().handle(null);
		}
		if(show && folderText.getOpacity() == 0.0) {
			fade = new FadeTransition(Duration.millis(125), folderText);
			fade.setFromValue(0);
			fade.setToValue(1);
			fade.setOnFinished((e) -> fade = null);
			fade.play();
		}
		else if(!show && folderText.getOpacity() != 0) {
			fade = new FadeTransition(Duration.millis(125), folderText);
			fade.setFromValue(1);
			fade.setToValue(0);
			fade.setOnFinished((e) -> fade = null);
			fade.play();
		}
	}

	private void updateFolderIcon() {
		
		if (mouseHovering || dragHovering || service.isActive(target)) {
			folderIcon.setTranslateX(16);
			folderIcon.setIconCode(FontAwesomeSolid.FOLDER_OPEN);
			
			showFolderText(mouseHovering && !dragHovering && !service.isActive(target));
		}
		else {			
			showFolderText(false);
			folderIcon.setTranslateX(0);
			folderIcon.setIconCode(FontAwesomeSolid.FOLDER);
		}
	}
}
