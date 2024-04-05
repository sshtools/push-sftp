package com.sshtools.pushsftp.jfx;

import java.util.ResourceBundle;

import org.controlsfx.control.TaskProgressView;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

import com.sshtools.jajafx.AbstractTile;

import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;

public class QueuePage extends AbstractTile<PushSFTPUIApp> {

	final static ResourceBundle RESOURCES = ResourceBundle.getBundle(QueuePage.class.getName());

	@FXML
	private TaskProgressView<TargetJob<?, ?>> jobs;

	@Override
	protected void onConfigure() {
		var service = getContext().getService();

		service.getJobs().addListener((ListChangeListener.Change<? extends TargetJob<?, ?>> c) -> {
			while (c.next()) {
				if (c.wasReplaced()) {
					jobs.getTasks().setAll(service.getJobs());
				} else {
					jobs.getTasks().addAll(c.getAddedSubList());
					jobs.getTasks().removeAll(c.getRemoved());
				}
			}
		});
		
		jobs.setRetainTasks(true);
		jobs.getTasks().addAll(service.getJobs());
		jobs.setGraphicFactory(j -> FontIcon.of(FontAwesomeSolid.UPLOAD, 24));
	}

	@FXML
	private void back(ActionEvent evt) {
		getTiles().remove(this);

	}

}
