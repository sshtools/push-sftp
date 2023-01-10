package com.sshtools.pushsftp.commands;

import java.util.concurrent.Callable;

import com.sshtools.client.sftp.SftpClient;
import com.sshtools.client.tasks.FileTransferProgress;
import com.sshtools.common.util.FileUtils;

import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "get", mixinStandardHelpOptions = true, description = "Download remote file")
public class Get extends SftpCommand implements Callable<Integer> {

	@Parameters(index = "0", arity = "1", description = "File to download")
	private String file;

	@Override
	public Integer call() throws Exception {
		SftpClient sftp = getSftpClient();

		sftp.getFiles(file, new FileTransferProgress() {
			
			long started = System.currentTimeMillis();
			long bytesTotal;
			public void started(long bytesTotal, String remoteFile) {
				System.out.print(String.format("Downloading %s", FileUtils.getFilename(remoteFile)));
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
			};
		});

		return 0;
	}
}