package com.sshtools.pushsftp.jfx;

import java.io.Closeable;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class FileTransferService implements Closeable {

	private ObservableList<Callable<Void>> jobs = FXCollections.observableArrayList();
	private ScheduledExecutorService service;
	private BooleanProperty busy = new SimpleBooleanProperty();
	
	public FileTransferService() {
		service = Executors.newSingleThreadScheduledExecutor();
	}
	
	public ReadOnlyBooleanProperty busyProperty() {
		return busy;
	}
	
	public void submit(Callable<Void> task) {
		jobs.add(task);
		service.submit(() -> {
			try {
				Platform.runLater(() -> busy.set(true));
				task.call();
			}
			catch(Exception e) {
				e.printStackTrace();
			} finally {
				Platform.runLater(() -> busy.set(false));
			}
		});
	}

	@Override
	public void close() {
		service.shutdown();
	}
}
