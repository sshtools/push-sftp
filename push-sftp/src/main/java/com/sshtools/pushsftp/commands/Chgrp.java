package com.sshtools.pushsftp.commands;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "chgrp", usageHelpAutoWidth = true, mixinStandardHelpOptions = false, description = "Change group of file path")
public class Chgrp extends SftpCommand  {

	@Option(names = { "-h" }, description = "Do not follow symlinks")
	private boolean dontFollowSymlinks;

	@Parameters(index = "0", description = "The GID or name of the group")
	private String gid;

	@Parameters(index = "1", arity = "1", description = "Path to change group of")
	private String path;
	
	public Chgrp() {
		super(FilenameCompletionMode.REMOTE);
	}

	@Override
	protected Integer onCall() throws Exception {
		expandRemoteAndDo((fp) -> {
			getSftpClient().chgrp(fp, gid);
		}, false, path);
		return 0;
	}
}
