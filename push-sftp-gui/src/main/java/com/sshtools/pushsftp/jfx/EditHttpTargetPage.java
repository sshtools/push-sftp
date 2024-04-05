package com.sshtools.pushsftp.jfx;

import static com.sshtools.jajafx.FXUtil.optionalText;
import static com.sshtools.jajafx.FXUtil.textOrPrompt;

import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.Consumer;

import com.sshtools.pushsftp.jfx.HttpTarget.HttpTargetBuilder;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;

public class EditHttpTargetPage extends AbstractEditTargetPage<HttpTarget> {

	final static ResourceBundle RESOURCES = ResourceBundle.getBundle(EditHttpTargetPage.class.getName());

	@FXML
	TextField name;
	@FXML
	TextField email;
	@FXML
	TextField url;
	@FXML
	TextField displayName;

	@FXML
	private void save() {
		var bldr = createTarget();
		onSave.accept(bldr.build());
		getTiles().remove(this);
		getTiles().prev();
	}

	private HttpTargetBuilder createTarget() {
		return new HttpTargetBuilder().
				fromTarget(target).
				withEmail(textOrPrompt(email)).
				withName(textOrPrompt(name)).
				withUrl(textOrPrompt(url)).
				withDisplayName(optionalText(displayName));
	}

	@Override
	protected void onEditConfigure() {
		name.setPromptText(System.getProperty("user.name"));
	}

	@Override
	protected void onSetTarget(HttpTarget target, Consumer<HttpTarget> onSave, Optional<Consumer<HttpTarget>> onDelete) {
		displayName.setText(target.displayName().orElse(""));
		name.setText(target.name());
		email.setText(target.email());
		var uri = target.uri();
		url.setText(uri == null ? "" : uri.toString());
		url.textProperty().addListener((c, o, n) -> rebuildDisplayNamePrompt());
		rebuildDisplayNamePrompt();
	}

	private void rebuildDisplayNamePrompt() {
		try {
			displayName.setPromptText(HttpTarget.httpDisplayName(new URL(url.getText()).toURI()));
		} catch (Exception e) {
			displayName.setPromptText("");
		}
	}
}
