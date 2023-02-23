package com.sshtools.pushsftp.commands;

import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "umask", usageHelpAutoWidth = true, mixinStandardHelpOptions = true, description = "Remove file")
public class Umask extends SftpCommand {

	@Parameters(index = "0", arity = "1", description = "Set the umask")
	private String umask;

	@Override
	public Integer call() throws Exception {
		getSftpClient().umask(umask);
		return 0;
	}
	
	
}
