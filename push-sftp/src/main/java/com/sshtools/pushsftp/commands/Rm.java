package com.sshtools.pushsftp.commands;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "rm", usageHelpAutoWidth = true, mixinStandardHelpOptions = true, description = "Remove file")
public class Rm extends SftpCommand {

	@Parameters(index = "0", arity = "1", description = "File to remove")
	private String file;
	
	@Option(names = "-f", description = "force deletion of children")
	private boolean force;
	
	@Option(names = "-r", description = "recursively delete directory and all children.")
	private boolean recursive;
	
	@Override
	protected Integer onCall() throws Exception {
		var expandedPath = expandRemoteSingle(file);
		getTerminal().messageln("Removing {0}", expandedPath);
		getSftpClient().rm(expandedPath, force, recursive);
		return 0;
	}

}
