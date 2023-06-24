package com.sshtools.pushsftp.commands;

import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.Callable;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "put", usageHelpAutoWidth = true, mixinStandardHelpOptions = true, description = "Upload local file.")
public class Put extends SftpCommand implements Callable<Integer> {

	@Parameters(index = "0", arity = "1..", description = "File to upload")
	private Path[] files;

	@Parameters(index = "1", arity = "0..1", description = "The remote path")
	private Optional<String> destination;

	@Option(names = { "-T", "--timing" }, description = "time the transfer operation")
	boolean timing;

	@Option(names = { "-a", "--async-requests" }, description = "the number of async requests to send", defaultValue = "0")
	int outstandingRequests;
	
	@Option(names = { "-b", "--blocksize" }, description = "the block size to use", defaultValue = "0")
	int blocksize; 
	
	public Put() {
		super(FilenameCompletionMode.LOCAL_THEN_REMOTE);
	}
	
	@Override
	protected Integer onCall() throws Exception {

		var sftp = getSftpClient();
		if(blocksize > 0) {
			sftp.setBlockSize(blocksize);
		}
		if(outstandingRequests > 0) {
			sftp.setMaxAsyncRequests(outstandingRequests);
		}
		
		var target = destination.orElse(sftp.pwd());

		try(var progress = io().progressBuilder().withInterruptable().withTiming(timing).withRateLimit().build()) {
			expandLocalAndDo((path) -> {
				sftp.put(path.toString(), target, fileTransferProgress(getRootCommand().io(), progress, "Uploading {0}"));
			}, true, files);
		}
		
		return 0;
	}
}