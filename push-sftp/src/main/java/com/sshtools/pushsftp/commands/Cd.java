package com.sshtools.pushsftp.commands;

import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "cd", mixinStandardHelpOptions = true, description = "Change remote directory")
public class Cd extends SftpCommand {

	
	@Parameters(index = "0", paramLabel="PATH", description = "change directory to PATH", defaultValue = ".")
	String path;
	
	@Override
	public Integer call() throws Exception {

		getSftpClient().cd(path);
		return 0;
	}
}
