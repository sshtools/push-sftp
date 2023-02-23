package com.sshtools.pushsftp.commands;

import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "rm", usageHelpAutoWidth = true, mixinStandardHelpOptions = true, description = "Remove file")
public class Rm extends SftpCommand {

	@Parameters(index = "0", arity = "1", description = "File to remove")
	private String file;
	
	@Override
	public Integer call() throws Exception {
		getSftpClient().rm(file);
		return 0;
	}

}
