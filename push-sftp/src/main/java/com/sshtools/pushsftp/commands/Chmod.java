package com.sshtools.pushsftp.commands;

import com.sshtools.common.sftp.PosixPermissions.PosixPermissionsBuilder;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "chmod", usageHelpAutoWidth = true, mixinStandardHelpOptions = false, description = "Change permissions of file path")
public class Chmod extends SftpCommand  {

	@Option(names = { "-h" }, description = "Do not follow symlinks")
	private boolean dontFollowSymlinks;

	@Parameters(index = "0", description = "The new permissions")
	private String perms;

	@Parameters(index = "1", arity = "1", description = "Path to change group of")
	private String path;

	@Override
	protected Integer onCall() throws Exception {
		expand(path, (fp) -> getSftpClient().chmod(PosixPermissionsBuilder.create().
					withChmodArgumentString(perms).build(), fp), false);
		return 0;
	}

}
