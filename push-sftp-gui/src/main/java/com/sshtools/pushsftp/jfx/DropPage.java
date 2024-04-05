package com.sshtools.pushsftp.jfx;

import java.util.ResourceBundle;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;

import org.controlsfx.control.NotificationPane;

import com.sshtools.jajafx.AboutPage;
import com.sshtools.jajafx.AbstractTile;
import com.sshtools.jajafx.FXUtil;
import com.sshtools.jajafx.PageTransition;
import com.sshtools.jajafx.PrefBind;
import com.sshtools.jajafx.ScrollStack;
import com.sshtools.pushsftp.jfx.FileTransferService.TransferUnit;

import eu.hansolo.medusa.Gauge;
import javafx.beans.binding.Bindings;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;

public class DropPage extends AbstractTile<PushSFTPUIApp> implements PreferenceChangeListener {

	final static ResourceBundle RESOURCES = ResourceBundle.getBundle(DropPage.class.getName());

	@FXML
	private Label text;
	@FXML
	private HBox progressContainer;

	@FXML
	private ScrollStack scrollStack;

	@FXML
	private BorderPane dropTop;
	
	@FXML
	private Gauge progressGauge;
	
	@FXML
	private Gauge speedGauge;
	
	@FXML
	private Hyperlink scrollPrevious;
	
	@FXML
	private Hyperlink scrollNext;

	
	NotificationPane  notificationPane;

	private TransferUnit transferUnit;
	private PrefBind prefBind;

	@Override
	protected void onConfigure() {
		
		prefBind = new PrefBind(getContext().getContainer().getAppPreferences());
		
		var service = getContext().getService();
		var targets = service.getTargets();
		
		targets.forEach(t -> scrollStack.add(new DropTarget().setup(t, getContext())));
		targets.addListener((ListChangeListener.Change<? extends Target> c) -> {
			while (c.next()) {
				if(c.wasReplaced()) {
					var ai = c.getAddedSubList().iterator();
					for (var t : c.getRemoved()) {
						var old = findDropTarget(t);
						var newTarget = ai.next();
						scrollStack.set(scrollStack.indexOf(old), new DropTarget().setup(newTarget, getContext()));
					}
				}
				else {
					for(var t : c.getRemoved()) {
						scrollStack.remove(findDropTarget(t));
					}
					for (var t : c.getAddedSubList()) {
						scrollStack.add(new DropTarget().setup(t, getContext()));
					}
				}
			}
		});
		
		FXUtil.clipChildren(scrollStack, 0);
		
		progressGauge.setMaxValue(100);
		service.busyProperty().addListener((c,o,n) ->{
			if(n) {
				notificationPane.hide();			
				resetGauges(); 
			}
		});
		service.summaryProperty().addListener((c,o,n) -> {
			if(service.busyProperty().get())
				updateGauges(); 
		});
		
		scrollPrevious.visibleProperty().bind(Bindings.not(scrollStack.showingFirstProperty()));
		scrollNext.visibleProperty().bind(Bindings.not(scrollStack.showingLastProperty()));
		
		progressContainer.disableProperty().bind(Bindings.not(service.busyProperty()));
		
		var dropTopParent = dropTop.getParent();
		notificationPane = new NotificationPane(dropTop);
		
		((AnchorPane)dropTopParent).getChildren().setAll(notificationPane);
		AnchorPane.setBottomAnchor(notificationPane, 0d);
		AnchorPane.setTopAnchor(notificationPane, 0d);
		AnchorPane.setLeftAnchor(notificationPane, 0d);
		AnchorPane.setRightAnchor(notificationPane, 0d);
		
		resetGauges();
		updateLabels();
		updateGauges();
		
		getContext().getContainer().getAppPreferences().addPreferenceChangeListener(this);
		
		prefBind.bind(scrollStack.indexProperty(), "scrollStack");
	}

	private void resetGauges() {
		speedGauge.setBarColor(Color.valueOf("#0078d7"));
		progressGauge.setTitle("");
		progressGauge.setBarColor( Color.valueOf("#0078d7"));
		speedGauge.setMaxValue(10);
		speedGauge.setValue(0);
		progressGauge.setValue(0);
	}
	
	private void updateLabels() {
		transferUnit = TransferUnit.valueOf(getContext().getContainer().getAppPreferences().get("transferSpeedUnits", TransferUnit.MB_S.name()));
		speedGauge.setMaxValue(10);
		speedGauge.setUnit(OptionsPage.RESOURCES.getString("transferSpeedUnits." + transferUnit.name()));
	}
	
	private void updateGauges() {
		var summary = getContext().getService().summaryProperty().get();
		progressGauge.setTitle(summary.size() == 0 ? "" :  summary.timeRemainingString());
		var speed = summary.transferRate(transferUnit);
		if(speed > speedGauge.getMaxValue())
			speedGauge.setMaxValue(speed);
		speedGauge.setValue(speed);
		if(summary.percentage() == 100) {
			if(progressGauge.getValue() != progressGauge.getCurrentValue()) {
				/* NOTE
				 * 
				 * There appears to be a bug in Medusa gauges animation, that when a value is
				 * set while it is animation, the value might get missed, leaving
				 * the gauge needle at the previous position.
				 *
				 * After trying many different things, this is the only hack I found
				 * that seems to correct it.
				 * 
				 * It is only noticeable for the final value (i.e. when upload has finished),
				 * and for small files (i.e. one or two update events).
				 * 
				 * We first detect if we are actually animating when the progress has finished,
				 * and if so reset the animation duration before setting the value.
				 * 
				 * This way we get to keep animation, and still have the correct "100%" value
				 * at the end of an upload.
				 */
				var dur = progressGauge.getAnimationDuration();
				progressGauge.setAnimationDuration(0);
				progressGauge.setValue(summary.percentage());
				progressGauge.setAnimationDuration(dur);
			}
			speedGauge.setBarColor(Color.GREEN.darker());
			progressGauge.setBarColor(Color.GREEN.darker());
		}
		progressGauge.setValue(summary.percentage());
	}
	
	@Override
	public void close() {
		prefBind.close();
		getContext().getContainer().getAppPreferences().removePreferenceChangeListener(this);
	}

	@Override
	public void preferenceChange(PreferenceChangeEvent evt) {
		if(evt.getKey().equals("transferSpeedUnits")) {
			FXUtil.maybeQueue(() -> updateLabels());
		}		
	}

	private DropTarget findDropTarget(Target target) {
		for(var child : scrollStack.getNodes()) {
			if(target.equals(((DropTarget)child).getTarget())) {
				return (DropTarget)child;
			}
		}
		throw new IllegalArgumentException("No such panel for target. " + target);
	}

	@FXML
	void addTarget(ActionEvent evt) {
		getTiles().popup(TargetTypePage.class, PageTransition.FROM_RIGHT);	
	}

	@FXML
	void next(ActionEvent evt) {
		scrollStack.next();
	}

	@FXML
	void previous(ActionEvent evt) {
		scrollStack.previous();
	}

	@FXML
	void about(ActionEvent evt) {
		getTiles().popup(AboutPage.class, PageTransition.FROM_LEFT);
	}

	@FXML
	void queue(ActionEvent evt) {
		getTiles().popup(QueuePage.class, PageTransition.FROM_LEFT);
	}

	@FXML
	void options(ActionEvent evt) {
		getTiles().popup(OptionsPage.class, PageTransition.FROM_LEFT);
	}

}
