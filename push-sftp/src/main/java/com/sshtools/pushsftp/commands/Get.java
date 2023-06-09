package com.sshtools.pushsftp.commands;

import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.Callable;

import com.sshtools.client.sftp.SftpClient;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "get", usageHelpAutoWidth = true, mixinStandardHelpOptions = true, description = "Download remote file")
public class Get extends SftpCommand implements Callable<Integer> {

	@Parameters(index = "0", arity = "1..", description = "File(s) to download")
	private String[] remotePaths;

	@Parameters(index = "1", arity = "0..1", description = "Local directory to download to")
	private Optional<Path> localPath;

	@Option(names = { "-T", "--timing" }, description = "time the transfer operation")
	boolean timing;

	@Option(names = { "-a", "--async-requests" }, description = "the number of async requests to send", defaultValue = "0")
	int outstandingRequests;
	
	@Option(names = { "-b", "--blocksize" }, description = "the block size to use", defaultValue = "0")
	int blocksize; 
	
	
	@Override
	protected Integer onCall() throws Exception {
		SftpClient sftp = getSftpClient();
		if(blocksize > 0) {
			sftp.setBlockSize(blocksize);
		}
		if(outstandingRequests > 0) {
			sftp.setMaxAsyncRequests(outstandingRequests);
		}
		var expandedLocalPath = expandLocalSingleOr(localPath);
		try(var progress = getTerminal().progressBuilder().withRateLimit().withTiming(timing).withInterruptable().build()) {
			expandRemoteAndDo(remotePath -> {
				if(expandedLocalPath.isPresent())
					sftp.get(remotePath, expandedLocalPath.get().toString(), fileTransferProgress(getRootCommand().getTerminal(), progress, "Downloading {0}"));
				else
					sftp.get(remotePath, fileTransferProgress(getRootCommand().getTerminal(), progress, "Downloading {0}"));
			}, true, remotePaths);
		}

		return 0;
	}
}