package com.sshtools.pushsftp.commands;

import java.util.Objects;
import java.util.concurrent.Callable;

import com.sshtools.client.sftp.SftpClient;
import com.sshtools.client.tasks.FileTransferProgress;
import com.sshtools.common.util.FileUtils;

import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "put", mixinStandardHelpOptions = true, description = "Upload local file.")
public class Put extends SftpCommand implements Callable<Integer> {

	@Parameters(index = "0", arity = "1", description = "File to upload")
	private String file;

	@Parameters(index = "1", arity = "0..1", description = "The remote path")
	private String destination;

	@Override
	public Integer call() throws Exception {

		SftpClient sftp = getSftpClient();
		
		String target = Objects.nonNull(destination) ? destination : sftp.pwd();
		sftp.putFiles(file, target, new FileTransferProgress() {
			
			long started = System.currentTimeMillis();
			long bytesTotal;
			public void started(long bytesTotal, String remoteFile) {
				System.out.print(String.format("Uploading %s", FileUtils.getFilename(remoteFile)));
				this.bytesTotal = bytesTotal;
			};

			public boolean isCancelled() {
				return false;
			};

			public void progressed(long bytesSoFar) {
				report(bytesSoFar, bytesTotal, started);
			};

			public void completed() {
				report(bytesTotal, bytesTotal, started);
				System.out.println();
			};
		});

		return 0;
	}
}