package com.sshtools.pushsftp.commands;

import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "cd", usageHelpAutoWidth = true, mixinStandardHelpOptions = true, description = "Change remote directory")
public class Cd extends SftpCommand {

	
	@Parameters(index = "0", arity="1", paramLabel="PATH", description = "change directory to PATH", defaultValue = ".")
	String path;
	
	@Override
	protected Integer onCall() throws Exception {
		var expandedPath = expandRemoteSingle(path);
		getSftpClient().cd(expandedPath);
		return 0;
	}
}
