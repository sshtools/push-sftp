package com.sshtools.pushsftp.commands;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "ln", usageHelpAutoWidth = true, mixinStandardHelpOptions = true, description = "Link remote file")
public class Ln extends SftpCommand {

	@Parameters(index = "0", arity = "1", description = "File to link to")
	private String linkTarget;

	@Parameters(index = "1", arity = "1", description = "Path of link file")
	private String link;
	
	@Option(names = "-s", description = "symbolic link")
	private boolean symobolic;
	
	@Override
	protected Integer onCall() throws Exception {
		var expandedLinkPath = expandRemoteSingle(link);
		var expandedLinkTargetPath = expandRemoteSingle(linkTarget);
		if(symobolic)
			getSftpClient().hardlink(expandedLinkPath, expandedLinkTargetPath);
		else
			getSftpClient().symlink(expandedLinkPath, expandedLinkTargetPath);
		return 0;
	}

}
