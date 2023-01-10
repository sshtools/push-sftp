package com.sshtools.pushsftp.commands;

import picocli.CommandLine.Command;

@Command(name = "lpwd", mixinStandardHelpOptions = true, description = "Display current local directory")
public class Lpwd extends SftpCommand {

	@Override
	public Integer call() throws Exception {

		System.out.println(getSftpClient().lpwd());
		return 0;
	}
}
