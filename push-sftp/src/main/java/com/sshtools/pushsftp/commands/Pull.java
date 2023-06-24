package com.sshtools.pushsftp.commands;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.Optional;

import com.sshtools.client.sftp.RemoteHash;
import com.sshtools.client.tasks.PullTask.PullTaskBuilder;
import com.sshtools.common.ssh.SshException;
import com.sshtools.sequins.Progress.Level;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "pull", usageHelpAutoWidth = true, mixinStandardHelpOptions = true, description = "Pull file from remote")
public class Pull extends SftpCommand {

	@Parameters(arity = "1..", paramLabel = "FILE", description = "one or more files/folders to transfer")
	String[] files;

	@Option(names = { "-c",
			"--chunks" }, paramLabel = "COUNT", description = "the number of concurrent parts (chunks) to transfer")
	int chunks = 3;

	@Option(names = { "-M", "--multiplex" }, description = "multiplex channels over the same connection for each chunk")
	boolean multiplex;

	@Option(names = { "-v", "--verify" }, description = "verify the integrity of the remote file")
	boolean verifyIntegrity = false;

	@Option(names = { "-d", "--digest" }, paramLabel = "md5|sha1|sha256|sha512", description = "The digest to use")
	RemoteHash digest = RemoteHash.md5;

	@Option(names = {
			"-G", "--ignore-integrity" }, description = "ignore integrity check if remote server does not support a suitable SFTP extension")
	boolean ignoreIntegrity = false;

	@Option(names = { "-r",
			"--local-dir" }, paramLabel = "PATH", description = "the directory locally you want to transfer the files to")
	Optional<Path> localFolder;

	@Option(names = { "-a", "--async-requests" }, description = "the number of async requests to send", defaultValue = "0")
	int outstandingRequests;
	
	@Option(names = { "-b", "--blocksize" }, description = "the block size to use", defaultValue = "0")
	int blocksize; 
	
	@Option(names = { "-T", "--timing" }, description = "time the transfer operation")
	boolean timing;
	
	@Option(names = { "-B", "--verbose" }, description = "verbose progress output")
	boolean verboseOutput;
	
	public Pull() {
		super(FilenameCompletionMode.REMOTE);
	}
	
	@Override
	protected Integer onCall() throws Exception {

		try (var progress = io().progressBuilder().withTiming(timing).withRateLimit().build()) {
			getSshClient().runTask(PullTaskBuilder.create().
				withClients((idx) -> {
					if (multiplex || idx == 0)
						return getSshClient();
					else {
						try {
							return getRootCommand().connect(false, idx < 2);
						} catch (IOException e) {
							throw new UncheckedIOException(e);
						} catch (SshException e) {
							throw new IllegalStateException(e);
						}
					}
				}).
				withPrimarySftpClient(getSftpClient()).
				withPaths(files).
				withChunks(chunks).
				withDigest(digest).
				withBlocksize(blocksize).
				withAsyncRequests(outstandingRequests).
				withLocalFolder(expandLocalSingleOr(localFolder)).
				withIntegrityVerification(verifyIntegrity).
				withIgnoreIntegrity(ignoreIntegrity).
				withVerboseOutput(verboseOutput).
				withProgressMessages((fmt, args) -> progress.message(Level.NORMAL, fmt, args)).
				withProgress(fileTransferProgress(getRootCommand().io(), progress, "Downloading {0}")).build());
		}

		return 0;
	}
}
