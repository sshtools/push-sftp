package com.sshtools.pushsftp.commands;

import com.sshtools.client.sftp.SftpClient;

import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "mkdir", usageHelpAutoWidth = true, mixinStandardHelpOptions = true, description = "Create directory")
public class Mkdir extends SftpCommand {

	@Parameters(index = "0", arity = "1", description = "Directory to create")
	private String directory;
	
	public Mkdir() {
		super(FilenameCompletionMode.DIRECTORIES_REMOTE);
	}
	
	@Override
	protected Integer onCall() throws Exception {
		SftpClient sftp = getSftpClient();
		sftp.mkdir(expandRemoteSingle(directory));
		return 0;
	}

}
