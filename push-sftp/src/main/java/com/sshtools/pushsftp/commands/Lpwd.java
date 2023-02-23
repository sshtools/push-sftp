package com.sshtools.pushsftp.commands;

import picocli.CommandLine.Command;

@Command(name = "lpwd", usageHelpAutoWidth = true, mixinStandardHelpOptions = true, description = "Display current local directory")
public class Lpwd extends SftpCommand {

	@Override
	public Integer call() throws Exception {
		getTerminal().messageln("Local working directory: {0}", getSftpClient().lpwd());
		return 0;
	}
}
