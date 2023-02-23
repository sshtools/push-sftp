package com.sshtools.pushsftp.commands;

import com.sshtools.pushsftp.PSFTPCommands;

import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "help", helpCommand = true, usageHelpAutoWidth = true, mixinStandardHelpOptions = false, description = "Print this help")
public class Help extends SftpCommand {

	@Override
	public Integer call() throws Exception {
		PSFTPCommands container = getInteractiveCommand();
		CommandLine.usage(container, System.out);
		return 0;
	}

}
