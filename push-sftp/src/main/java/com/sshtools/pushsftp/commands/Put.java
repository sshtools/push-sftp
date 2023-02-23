package com.sshtools.pushsftp.commands;

import java.util.Objects;
import java.util.concurrent.Callable;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "put", usageHelpAutoWidth = true, mixinStandardHelpOptions = true, description = "Upload local file.")
public class Put extends SftpCommand implements Callable<Integer> {

	@Parameters(index = "0", arity = "1", description = "File to upload")
	private String file;

	@Parameters(index = "1", arity = "0..1", description = "The remote path")
	private String destination;

	@Option(names = { "-T", "--timing" }, description = "time the transfer operation")
	boolean timing;

	@Override
	public Integer call() throws Exception {

		var sftp = getSftpClient();
		var target = Objects.nonNull(destination) ? destination : sftp.pwd();

		try(var progress = getTerminal().progressBuilder().withInterruptable().withTiming(timing).withRateLimit().build()) {
			sftp.putFiles(file, target, fileTransferProgress(progress, "Uploading {0}"));
		}

		return 0;
	}
}