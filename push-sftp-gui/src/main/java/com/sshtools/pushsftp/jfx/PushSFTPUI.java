package com.sshtools.pushsftp.jfx;

import java.text.MessageFormat;
import java.util.Optional;

import com.sshtools.common.logger.Log;
import com.sshtools.common.logger.Log.Level;
import com.sshtools.jajafx.JajaApp;
import com.sshtools.jaul.AppCategory;
import com.sshtools.jaul.ArtifactVersion;
import com.sshtools.jaul.JaulApp;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.IVersionProvider;
import picocli.CommandLine.Option;

@Command(name = "filedrop", mixinStandardHelpOptions = true, description = "Simple graphical user interface for push files to an SFTP server as fast as possible.", versionProvider = PushSFTPUI.Version.class)
@JaulApp(id = "com.sshtools.FileDrop", category = AppCategory.GUI, updaterId = "54", updatesUrl = "https://sshtools-public.s3.eu-west-1.amazonaws.com/push-sftp-gui/${phase}/updates.xml")
public class PushSFTPUI extends JajaApp<PushSFTPUIApp> {

	public final static class Version implements IVersionProvider {
		@Override
		public String[] getVersion() throws Exception {
			var synergyVersion = ArtifactVersion.getVersion("com.sshtools", "maverick-synergy-client");
			if(synergyVersion.equals("DEV_VERSION")) {
				synergyVersion = ArtifactVersion.getVersion("com.sshtools.hotfixes", "maverick-synergy-client");
			}
			return new String[] {
					ArtifactVersion.getVersion("filedrop", "com.sshtools", "push-sftp-gui"),
					MessageFormat.format("using Maverick Synergy {0}",synergyVersion)
					};
		}
	}
	
	public final static class PushSFTPUIBuilder extends JajaAppBuilder<PushSFTPUI, PushSFTPUIBuilder, PushSFTPUIApp> {
		
		private PushSFTPUIBuilder() {
		}
		
		public static PushSFTPUIBuilder create() {
			return new PushSFTPUIBuilder();
		}
		
		@Override
		public PushSFTPUI build() {
			return new PushSFTPUI(this);
		}
	}
	
	@Option(names = { "-m", "--ssh-log" }, paramLabel = "LEVEL", description = "Enable Maverick API debugging (for SSH related output).")
	Optional<Level> sshLog;

	PushSFTPUI(PushSFTPUIBuilder builder) {
		super(builder);
	}
	
	@Override
	protected void beforeCall() throws Exception {
		sshLog.ifPresent(l -> Log.enableConsole(Level.DEBUG));
	}

	public static void main(String[] args) {
		var bldr = PushSFTPUIBuilder.create().
				withInceptionYear(2023).
				withApp(PushSFTPUIApp.class).
				withAppResources(PushSFTPUIApp.RESOURCES);
		System.exit(new CommandLine(bldr.build()).execute(args));
	}
}
