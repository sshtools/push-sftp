package com.sshtools.pushsftp.commands;

import picocli.CommandLine.Command;

@Command(name = "bye", usageHelpAutoWidth = true, mixinStandardHelpOptions = false, description = "Quit interactive command")
public class Bye extends SftpCommand {

	@Override
	public Integer call() throws Exception {
		System.exit(0);
		return null;
	}

	
}
