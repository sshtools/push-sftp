package com.sshtools.pushsftp.commands;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "rename", usageHelpAutoWidth = true, mixinStandardHelpOptions = true, description = "Symlink remote file")
public class Rename extends SftpCommand {

	@Parameters(index = "0", arity = "1", description = "Old path")
	private String oldPath;

	@Parameters(index = "1", arity = "1", description = "New path")
	private String newPath;
	
	@Option(names = "-p", description = "use POSIX extension")
	private boolean posix;
	
	@Override
	protected Integer onCall() throws Exception {
		var expandedOldPath = expandRemoteSingle(oldPath);
		var expandedNewPath = expandRemoteSingle(newPath);
		getSftpClient().rename(expandedOldPath, expandedNewPath, posix);
		return 0;
	}

}
