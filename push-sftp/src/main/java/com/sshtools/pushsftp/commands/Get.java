package com.sshtools.pushsftp.commands;

import java.util.concurrent.Callable;

import com.sshtools.client.sftp.SftpClient;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "get", usageHelpAutoWidth = true, mixinStandardHelpOptions = true, description = "Download remote file")
public class Get extends SftpCommand implements Callable<Integer> {

	@Parameters(index = "0", arity = "1", description = "File to download")
	private String file;

	@Option(names = { "-T", "--timing" }, description = "time the transfer operation")
	boolean timing;

	@Override
	public Integer call() throws Exception {
		SftpClient sftp = getSftpClient();

		try(var progress = getTerminal().progressBuilder().withRateLimit().withTiming(timing).withInterruptable().build()) {
			sftp.getFiles(file, fileTransferProgress(progress, "Downloading {0}"));
		}

		return 0;
	}
}