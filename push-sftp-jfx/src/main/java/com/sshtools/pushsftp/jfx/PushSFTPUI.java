package com.sshtools.pushsftp.jfx;

import com.sshtools.jajafx.JajaApp;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "psftp-gui", mixinStandardHelpOptions = true, description = "Simple graphic user interface for push files to an SFTP server as fast as possible.", versionProvider = AppVersion.class)
public class PushSFTPUI extends JajaApp<PushSFTPUIApp> {

	@Option(names = { "-m", "--maverick-debug" }, paramLabel = "PATH", description = "Enable Maverick API debugging (for SSH related output).")
	boolean maverickDebug;

	PushSFTPUI() {
		super(PushSFTPUIApp.class);
	}
	
	public static void main(String[] args) {
		System.exit(new CommandLine(new PushSFTPUI()).execute(args));
	}
}
