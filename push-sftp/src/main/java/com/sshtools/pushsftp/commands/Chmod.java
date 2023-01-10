package com.sshtools.pushsftp.commands;

import java.io.IOException;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "chmod", mixinStandardHelpOptions = false, description = "Change permissions of file path")
public class Chmod extends SftpCommand  {

	@Option(names = { "-h" }, description = "Do not follow symlinks")
	private boolean dontFollowSymlinks;

	@Parameters(index = "0", description = "The new permissions")
	private String perms;

	@Parameters(index = "1", arity = "1", description = "Path to change group of")
	private String path;

	@Override
	public Integer call() throws Exception {
		
		int actualPermissions = parsePermissions(perms);
		
		expand(path, (fp) -> {
			getSftpClient().chmod(actualPermissions, fp);
		}, false);
		return 0;
	}

	private int parsePermissions(String perms) throws IOException {
		
		if(perms.matches("\\d+")) {
			return Integer.parseInt(perms, 8);
		}
		else {
			throw new IOException("chmod only supports octal permissions");
		}
	}
}
