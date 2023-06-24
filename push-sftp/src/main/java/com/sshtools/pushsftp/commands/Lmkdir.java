package com.sshtools.pushsftp.commands;

import java.nio.file.Files;
import java.nio.file.Path;

import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "lmkdir", usageHelpAutoWidth = true, mixinStandardHelpOptions = true, description = "Create local directory")
public class Lmkdir extends SftpCommand {

	@Parameters(index = "0", arity = "1", description = "Directory to create")
	private Path directory;
	
	public Lmkdir() {
		super(FilenameCompletionMode.DIRECTORIES_LOCAL);
	}
	
	@Override
	protected Integer onCall() throws Exception {
		Files.createDirectories(expandLocalSingle(directory));
		return 0;
	}

}
