package com.sshtools.pushsftp.commands;

import java.nio.file.Path;

import com.sshtools.pushsftp.PSFTPInteractive;

import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "lcd", usageHelpAutoWidth = true, mixinStandardHelpOptions = true, description = "Change local directory")
public class Lcd extends SftpCommand {

	@Parameters(index = "0", arity="1", paramLabel="PATH", description = "change directory to PATH", defaultValue = ".")
	Path path;
	
	@Override
	protected Integer onCall() throws Exception {
		var resolvedPath = expandLocalSingle(path);
		getSftpClient().lcd(resolvedPath.toString());
		PSFTPInteractive cmd = getInteractiveCommand().rootCommand();
		cmd.setLocalDirectory(resolvedPath);
		return 0;
	}
}
