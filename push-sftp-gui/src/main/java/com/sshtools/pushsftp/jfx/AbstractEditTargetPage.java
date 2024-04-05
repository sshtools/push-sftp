package com.sshtools.pushsftp.jfx;

import java.text.MessageFormat;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.Consumer;

import com.sshtools.jajafx.AbstractTile;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Hyperlink;

public abstract class AbstractEditTargetPage<TARG extends Target>  extends AbstractTile<PushSFTPUIApp> {

	final static ResourceBundle RESOURCES = ResourceBundle.getBundle(AbstractEditTargetPage.class.getName());
	
	@FXML
	Hyperlink delete;

	Consumer<TARG> onSave;
	Optional<Runnable> onDelete;

	TARG target;

	@Override
	protected final void onConfigure() {
		delete.managedProperty().bind(delete.visibleProperty());
		onEditConfigure();
	}

	protected void onEditConfigure() {
		
	}

	public final void setTarget(TARG target, Consumer<TARG> onSave, Optional<Consumer<TARG>> onDelete) {
		this.onSave = onSave;
		this.target = target;
		this.onDelete = onDelete.map(r -> new Runnable() {
			@Override
			public void run() {
				r.accept(target);
			}
		});
		delete.setVisible(onDelete.isPresent());
		onSetTarget(target, onSave, onDelete);
	}

	@FXML
	void cancel() {
		getTiles().remove(this);
	}

	protected abstract void onSetTarget(TARG target, Consumer<TARG> onSave, Optional<Consumer<TARG>> onDelete);

	@FXML
	final void delete() {

		var alert = new Alert(AlertType.CONFIRMATION);
		alert.initOwner(getContext().getWindows().get(0).stage());
		alert.setTitle(RESOURCES.getString("delete.title"));
		alert.setHeaderText(MessageFormat.format(RESOURCES.getString("delete.subtitle"), target.bestDisplayName() ));
		alert.setContentText(MessageFormat.format(RESOURCES.getString("delete.text"), target.bestDisplayName() ));

		var result = alert.showAndWait();
		if (result.get() != ButtonType.OK){
			return;
		}
		
		onDelete.ifPresent(r -> r.run());
		getTiles().remove(this);
	}
}
