package com.sshtools.pushsftp.commands;

import java.io.File;

import com.sshtools.pushsftp.PSFTPInteractive;

import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "lcd", usageHelpAutoWidth = true, mixinStandardHelpOptions = true, description = "Change local directory")
public class Lcd extends SftpCommand {

	@Parameters(index = "0", paramLabel="PATH", description = "change directory to PATH", defaultValue = ".")
	String path;
	
	@Override
	protected Integer onCall() throws Exception {

		getSftpClient().lcd(path);
		PSFTPInteractive cmd = getInteractiveCommand().rootCommand();
		cmd.setLocalDirectory(new File(path));
		return 0;
	}
}
