package com.sshtools.pushsftp.commands;

import com.sshtools.client.sftp.SftpClient;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "rmdir", mixinStandardHelpOptions = true, description = "Remove directory")
public class Rmdir extends SftpCommand {

	@Parameters(index = "0", arity = "1", description = "Directory to remove")
	private String file;
	
	@Option(names = "-f", description = "force deletion of children")
	private boolean force;
	
	@Override
	public Integer call() throws Exception {
		
		SftpClient sftp = getSftpClient();
		sftp.rm(file, force, true);
		return 0;
	}

}