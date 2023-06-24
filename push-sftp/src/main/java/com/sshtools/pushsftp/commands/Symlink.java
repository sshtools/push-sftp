package com.sshtools.pushsftp.commands;

import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "symlink", usageHelpAutoWidth = true, mixinStandardHelpOptions = true, description = "Symlink remote file")
public class Symlink extends SftpCommand {

	@Parameters(index = "0", arity = "1", description = "File to link to")
	private String linkTarget;

	@Parameters(index = "1", arity = "1", description = "Path of link file")
	private String link;
	
	public Symlink() {
		super(FilenameCompletionMode.REMOTE);
	}
	
	@Override
	protected Integer onCall() throws Exception {
		var expandedLinkPath = expandRemoteSingle(link);
		var expandedLinkTargetPath = expandRemoteSingle(linkTarget);
			getSftpClient().symlink(expandedLinkPath, expandedLinkTargetPath);
		return 0;
	}

}
