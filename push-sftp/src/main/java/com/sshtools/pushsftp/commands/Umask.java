package com.sshtools.pushsftp.commands;

import com.sshtools.client.sftp.SftpClient;

import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "umask", mixinStandardHelpOptions = true, description = "Remove file")
public class Umask extends SftpCommand {

	@Parameters(index = "0", arity = "1", description = "Set the umask")
	private String umask;

	@Override
	public Integer call() throws Exception {
		
		SftpClient sftp = getSftpClient();
		sftp.umask(umask);
		return 0;
	}
	
	
}
