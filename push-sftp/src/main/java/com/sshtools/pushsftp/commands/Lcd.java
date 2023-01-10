package com.sshtools.pushsftp.commands;

import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "lcd", mixinStandardHelpOptions = true, description = "Change local directory")
public class Lcd extends SftpCommand {

	
	@Parameters(index = "0", paramLabel="PATH", description = "change directory to PATH", defaultValue = ".")
	String path;
	
	@Override
	public Integer call() throws Exception {

		getSftpClient().lcd(path);
		return 0;
	}
}
