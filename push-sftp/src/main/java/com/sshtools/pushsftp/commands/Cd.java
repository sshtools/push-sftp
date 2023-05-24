package com.sshtools.pushsftp.commands;

import com.sshtools.pushsftp.PSFTPInteractive;

import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "cd", usageHelpAutoWidth = true, mixinStandardHelpOptions = true, description = "Change remote directory")
public class Cd extends SftpCommand {

	
	@Parameters(index = "0", paramLabel="PATH", description = "change directory to PATH", defaultValue = ".")
	String path;
	
	@Override
	protected Integer onCall() throws Exception {

		getSftpClient().cd(path);
		PSFTPInteractive cmd = getInteractiveCommand().rootCommand();
		cmd.setRemoteDirectory(path);
		return 0;
	}
}
