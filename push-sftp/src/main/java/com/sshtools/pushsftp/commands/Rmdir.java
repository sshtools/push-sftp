package com.sshtools.pushsftp.commands;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "rmdir", usageHelpAutoWidth = true, mixinStandardHelpOptions = true, description = "Remove directory")
public class Rmdir extends SftpCommand {

	@Parameters(index = "0", arity = "1", description = "Directory to remove")
	private String file;
	
	@Option(names = "-f", description = "force deletion of children")
	private boolean force;
	
	@Override
	protected Integer onCall() throws Exception {
		getTerminal().messageln("Removing dir {0}", file);
		getSftpClient().rm(file, force, true);
		return 0;
	}

}
