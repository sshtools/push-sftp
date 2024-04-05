package com.sshtools.pushsftp.jfx;

import java.util.Optional;

import com.sshtools.jajafx.AbstractTile;
import com.sshtools.jajafx.PageTransition;
import com.sshtools.jajafx.PrefBind;
import com.sshtools.pushsftp.jfx.HttpTarget.HttpTargetBuilder;
import com.sshtools.pushsftp.jfx.SshTarget.SshTargetBuilder;

import javafx.beans.binding.Bindings;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.MouseEvent;

public class TargetTypePage extends AbstractTile<PushSFTPUIApp> {

	private PrefBind prefBind;

	@FXML
	ToggleGroup action;
	@FXML
	RadioButton ssh;
	@FXML
	RadioButton sfx;
	@FXML
	Hyperlink next;

	@Override
	protected void onConfigure() {
		next.disableProperty().bind(Bindings.isNull(action.selectedToggleProperty()));
		prefBind = new PrefBind(getContext().getContainer().getAppPreferences());
		prefBind.bind(action, "targetType");
	}

	@Override
	public void close() {
		prefBind.close();
	}

	@FXML
	private void ssh(MouseEvent evt) {
		ssh.setSelected(true);
		selectAndNext(evt);
	}

	@FXML
	private void sfx(MouseEvent evt) {
		sfx.setSelected(true);
		selectAndNext(evt);
	}

	@FXML
	private void selectAndNext(MouseEvent evt) {
		if (evt.getClickCount() == 2)
			next(null);
	}

	@FXML
	private void next(ActionEvent evt) {
		getTiles().remove(this);
		if(ssh.isSelected()) {
			getTiles().popup(EditSshTargetPage.class, PageTransition.FROM_RIGHT).setTarget(new SshTargetBuilder().build(),
					(newTarget) -> getContext().getService().getTargets().add(newTarget), Optional.empty());	
		}
		else {
			getTiles().popup(EditHttpTargetPage.class, PageTransition.FROM_RIGHT).setTarget(new HttpTargetBuilder().build(),
					(newTarget) -> getContext().getService().getTargets().add(newTarget), Optional.empty());
		}
	}

	@FXML
	private void back(ActionEvent evt) {
		getTiles().remove(this);
	}

}
