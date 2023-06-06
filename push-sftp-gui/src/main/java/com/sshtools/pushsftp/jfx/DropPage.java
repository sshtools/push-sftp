package com.sshtools.pushsftp.jfx;

import java.util.Optional;
import java.util.ResourceBundle;

import com.sshtools.jajafx.AboutPage;
import com.sshtools.jajafx.AbstractTile;
import com.sshtools.jajafx.Carousel;
import com.sshtools.jajafx.PageTransition;
import com.sshtools.jajafx.SequinsProgress;
import com.sshtools.pushsftp.jfx.Target.TargetBuilder;

import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class DropPage extends AbstractTile<PushSFTPUIApp> {

	final static ResourceBundle RESOURCES = ResourceBundle.getBundle(DropPage.class.getName());

	@FXML
	private Label text;

	@FXML
	private SequinsProgress progress;

	@FXML
	private Carousel carousel;

	@Override
	protected void onConfigure() {
		var targets = getContext().getService().getTargets();
		var carouselItems = carousel.getItems();
		targets.forEach(t -> carouselItems.add(new DropTarget().setup(t, getContext(), progress)));
		targets.addListener((ListChangeListener.Change<? extends Target> c) -> {
			while (c.next()) {
				if(c.wasReplaced()) {
					// A bit brute force, look for better way
					carouselItems.clear();
					targets.forEach(t -> carouselItems.add(new DropTarget().setup(t, getContext(), progress)));
				}
				else {
					for (var t : c.getAddedSubList()) {
						carouselItems.add(new DropTarget().setup(t, getContext(), progress));
					}
					for(var t : c.getRemoved()) {
						carouselItems.remove(findDropTarget(t));
					}
				}
			}
		});
	}
	
	private DropTarget findDropTarget(Target target) {
		for(var child : carousel.getItems()) {
			if(target.equals(((DropTarget)child).getTarget())) {
				return (DropTarget)child;
			}
		}
		throw new IllegalArgumentException("No such panel for target. " + target);
	}

	@FXML
	void addTarget(ActionEvent evt) {
		getTiles().popup(EditTargetPage.class, PageTransition.FROM_RIGHT).setTarget(TargetBuilder.builder().build(),
				(newTarget) -> getContext().getService().getTargets().add(newTarget), Optional.empty());
	}

	@FXML
	void about(ActionEvent evt) {
		getTiles().popup(AboutPage.class);
	}

	@FXML
	void options(ActionEvent evt) {
		getTiles().popup(OptionsPage.class);
	}

}
