package com.sshtools.pushsftp.commands;

import picocli.CommandLine.Command;

@Command(name = "pwd", mixinStandardHelpOptions = true, description = "Display current remote directory")
public class Pwd extends SftpCommand {

	@Override
	public Integer call() throws Exception {

		System.out.println(getSftpClient().pwd());
		return 0;
	}
}
