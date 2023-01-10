package com.sshtools.pushsftp.commands;

import com.sshtools.client.sftp.SftpClient;

import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "rm", mixinStandardHelpOptions = true, description = "Remove file")
public class Rm extends SftpCommand {

	@Parameters(index = "0", arity = "1", description = "File to remove")
	private String file;
	
	@Override
	public Integer call() throws Exception {
		
		SftpClient sftp = getSftpClient();
		sftp.rm(file);
		return 0;
	}

}
