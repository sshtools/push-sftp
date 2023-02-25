package com.sshtools.pushsftp.commands;

import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "help", helpCommand = true, usageHelpAutoWidth = true, mixinStandardHelpOptions = false, description = "Print this help")
public class Help extends SftpCommand {

	@Override
	protected Integer onCall() throws Exception {
		var container = getInteractiveCommand();
		CommandLine.usage(container, System.out);
		return 0;
	}

}
