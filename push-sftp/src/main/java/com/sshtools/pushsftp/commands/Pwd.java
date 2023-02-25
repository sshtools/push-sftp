package com.sshtools.pushsftp.commands;

import picocli.CommandLine.Command;

@Command(name = "pwd", usageHelpAutoWidth = true, mixinStandardHelpOptions = true, description = "Display current remote directory")
public class Pwd extends SftpCommand {
	
	@Override
	protected Integer onCall() throws Exception {
		getTerminal().messageln("Remote working directory: {0}", getSftpClient().pwd());
		return 0;
	}
}
