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

	@Option(names = { "-a", "--async-requests" }, description = "the number of async requests to send", defaultValue = "64")
	int outstandingRequests;
	
	@Option(names = { "-b", "--blocksize" }, description = "the block size to use", defaultValue = "32768")
	int blocksize; 
	
	
	@Override
	protected Integer onCall() throws Exception {
		SftpClient sftp = getSftpClient();
		sftp.setBlockSize(blocksize);
		sftp.setMaxAsyncRequests(outstandingRequests);
		
		try(var progress = getTerminal().progressBuilder().withRateLimit().withTiming(timing).withInterruptable().build()) {
			sftp.getFiles(file, fileTransferProgress(progress, "Downloading {0}"));
		}

		return 0;
	}
}