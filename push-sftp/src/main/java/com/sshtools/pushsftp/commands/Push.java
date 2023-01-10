package com.sshtools.pushsftp.commands;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import com.sshtools.client.SessionChannelNG;
import com.sshtools.client.SshClient;
import com.sshtools.client.scp.ScpClient;
import com.sshtools.client.scp.ScpClientIO;
import com.sshtools.client.sftp.RemoteHash;
import com.sshtools.client.sftp.SftpChannel;
import com.sshtools.client.sftp.SftpClient;
import com.sshtools.client.sftp.SftpFile;
import com.sshtools.client.sftp.TransferCancelledException;
import com.sshtools.client.shell.ExpectShell;
import com.sshtools.client.shell.ShellProcess;
import com.sshtools.client.shell.ShellTimeoutException;
import com.sshtools.client.tasks.FileTransferProgress;
import com.sshtools.client.tasks.ShellTask;
import com.sshtools.common.files.AbstractFile;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.sftp.SftpFileAttributes;
import com.sshtools.common.sftp.SftpStatusException;
import com.sshtools.common.ssh.ChannelOpenException;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.util.FileUtils;
import com.sshtools.common.util.UnsignedInteger64;
import com.sshtools.common.util.Utils;
import com.sshtools.pushsftp.util.ChunkInputStream;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "push", mixinStandardHelpOptions = true, description = "Push file to remote")
public class Push extends SftpCommand {

    @Parameters(paramLabel = "FILE", description = "one or more files/folders to transfer")
    String[] files;
    
    @Option(names = { "-c", "--chunks" }, paramLabel = "COUNT", description = "the number of concurrent parts (chunks) to transfer")
    int chunks = 3;
    
    @Option(names = { "-M", "--multiplex" }, description = "multiplex channels over the same connection for each chunk")
    boolean multiplex;
    
    @Option(names = { "-v", "--verify"}, description = "verify the integrity of the remote file")
    boolean verifyIntegrity = false;
    
    @Option(names = { "-d", "--digest"}, paramLabel = "md5|sha1|sha256|sha512", description = "The digest to use")
    RemoteHash digest = RemoteHash.md5;
    
    @Option(names = { "-V" }, description = "ignore integrity check if remote server does not support a suitable SFTP extension")
    boolean ignoreIntegrity = false;
    
    @Option(names = { "-F" }, description = "force the use of SFTP for all transfers (default is to use SCP)")
    boolean forceSFTP;
    
	@Option(names = { "-r", "--remote-dir" }, paramLabel = "PATH", description = "the directory on the remote host you want to transfer the files to")
    String remoteFolder;
	
    private boolean reportDone;
    private SftpFileAttributes remoteAttrs = null;
    LinkedList<SshClient> clients = new LinkedList<>();

	@Override
	public Integer call() {
		
		try {

			configureConnections();
			
			configureRemoteFolder();
			
			transferFiles();
			
			return 0;
		} catch(Throwable e) {
			throw new IllegalStateException(e.getMessage(), e);
		} finally {
			for(SshClient ssh : clients) {
				ssh.disconnect();
			}
			clients.clear();
		}
	}

	private void transferFiles() throws SftpStatusException, SshException, TransferCancelledException, IOException, PermissionDeniedException, ChannelOpenException {
		
		for(String file : files) {
			transferFile(file);
		}
	}

	private void configureConnections() throws IOException, SshException {
		
		if(multiplex) {
			System.out.println(String.format("Connecting to %s@%s:%d", getUsername(), getHost(), getPort()));
			SshClient ssh = getInteractiveCommand().getParentCommand().connect();
			for(int i=0;i<chunks;i++) {
				clients.add(ssh);
			}
		} else {
			System.out.println(String.format("Creating %d connections to %s@%s:%d", chunks, getUsername(), getHost(), getPort()));
			for(int i=0;i<chunks;i++) {
				clients.add(getInteractiveCommand().getParentCommand().connect(false, i==0));
			}
		}
	}

	private void configureRemoteFolder() throws IOException, SshException, PermissionDeniedException, SftpStatusException {
		
		SftpClient sftp = getSftpClient();
	
		if(Utils.isNotBlank(remoteFolder)) {
			remoteFolder = sftp.getAbsolutePath(remoteFolder);
		} else {
			remoteFolder = sftp.getAbsolutePath(".");
		}
		
		remoteAttrs = sftp.stat(remoteFolder);
		if(!remoteAttrs.isDirectory()) {
			throw new IOException("--remote-dir must be a directory!");
		}
		
		System.out.printf("The files will be transferred to %s%s", remoteFolder, System.lineSeparator());
		
	}

	private void transferFile(String file) throws SftpStatusException, SshException, TransferCancelledException, IOException, PermissionDeniedException, ChannelOpenException {
		
		SftpClient sftp = getSftpClient();
		
		AbstractFile localFile = sftp.getCurrentWorkingDirectory().resolveFile(file);
		
		if(!localFile.exists()) {
			throw new IOException(String.format("%s does not exist!", localFile.getName()));
		}

		System.out.printf("Total to transfer is %d bytes%s", localFile.length(), System.lineSeparator());
		
		if(chunks <= 1) {
			if(forceSFTP) {
				sendFileViaSFTP(localFile, "");
			} else {
				sendFileViaSCP(localFile, 
						FileUtils.checkEndsWithSlash(remoteFolder)
							+ localFile.getName());
			}
			
			verifyIntegrity(localFile);
		} else {
			
			checkErrors(sendChunks(localFile));
			
			combineChunks(localFile);
				
			verifyIntegrity(localFile);
		}
		
	}

	private void checkErrors(Collection<Throwable> errors) throws IOException {
		if(errors.isEmpty()) {
			return;
		}
		for(Throwable e : errors) {
			e.printStackTrace();
		}
		throw new IOException("Transfer could not be completed due to multiple errors!");
	}

	private void combineChunks(AbstractFile localFile) throws IOException {
		
		System.out.println("Combining parts into final file on remote machine");
		
		if(!performCopyData(localFile)) {
			performRemoteCat(localFile);
		}
	
	}
	
	private void performRemoteCat(AbstractFile localFile) throws IOException {
		
		SshClient ssh = getSshClient();

		ssh.runTask(new ShellTask(ssh) {

			private void execute(String command, ExpectShell shell) throws IOException, SshException {
				ShellProcess process = shell.executeCommand(command);
				process.drain();
				if(process.hasSucceeded()) {
					if(Utils.isNotBlank(process.getCommandOutput())) {
						System.out.println(process.getCommandOutput());
					}
				} else {
					if(Utils.isNotBlank(process.getCommandOutput())) {
						System.err.println(process.getCommandOutput());
					}
					throw new IOException("Command failed");
				}
			}
			@Override
			protected void onOpenSession(SessionChannelNG session)
					throws IOException, SshException, ShellTimeoutException {
				
				System.out.println("Opening shell");
				ExpectShell shell = new ExpectShell(this);
				execute(String.format("cd %s", remoteFolder), shell);
				System.out.println("Combining files");
				execute(String.format("cat %s_* > %s",localFile.getName(), localFile.getName()), shell);
				System.out.println("Deleting parts");
				execute(String.format("rm -f %s_*",localFile.getName(), localFile.getName()), shell);
			}
			
		});
		
	}

	private boolean performCopyData(AbstractFile localFile) throws IOException {
		
		List<Throwable> errors = new ArrayList<>();
		
		SftpClient sftp = getSftpClient();

		try {
			if(sftp.getSubsystemChannel().supportsExtension("copy-data")) {
				System.err.println("Remote server does not support copy-data SFTP extension");
				return false;
			}
			sftp.cd(remoteFolder);
			UnsignedInteger64 zero = new UnsignedInteger64(0);
			
			SftpFile remoteFile = sftp.getSubsystemChannel().openFile(
					FileUtils.checkEndsWithSlash(remoteFolder) + localFile.getName(), SftpChannel.OPEN_WRITE | 
					SftpChannel.OPEN_CREATE | SftpChannel.OPEN_TRUNCATE);
			
			long position = 0L;
			List<SftpFile> remoteChunks = new ArrayList<>();
			
			try {
				for(int chunk=1;chunk<=chunks;chunk++) {
					
					SftpFile remoteChunk = sftp.getSubsystemChannel().openFile(
							FileUtils.checkEndsWithSlash(remoteFolder) + localFile.getName() + "_part" + chunk, SftpChannel.OPEN_READ);
					remoteChunks.add(remoteChunk);
					try {
					   sftp.copyRemoteData(remoteChunk, zero, zero, remoteFile, new UnsignedInteger64(position));
					   System.out.printf("Copied part %d of %d to %s%s", chunk, chunks, localFile.getName(), System.lineSeparator());
					   position += remoteChunk.getAttributes().getSize().longValue();
					} catch(SftpStatusException e) { 
						if(e.getStatus()==SftpStatusException.SSH_FX_OP_UNSUPPORTED) {
							System.err.println("Remote server does not support copy-data SFTP extension");
							return false;
						} else {
							errors.add(e);
						}
					} finally {
						remoteChunk.close();
					}
				}
			} finally {
				remoteFile.close();
			}
			
			checkErrors(removeChunks(localFile, remoteChunks));
		} catch(Throwable e)  { 
			errors.add(e);
		} 
		
		checkErrors(errors);
		
		return true;
	}

	private Collection<Throwable> removeChunks(AbstractFile localFile, Collection<SftpFile> remoteChunks) throws IOException {
		
		List<Throwable> errors = new ArrayList<>();
		
		try {
			for(SftpFile remoteChunk : remoteChunks) {
				try {
					remoteChunk.delete();
				} catch (SftpStatusException | SshException e) {
					errors.add(e);
				}
			}
		} catch(Throwable e) {
			errors.add(e);
		}
		return errors;
	}

	private Collection<Throwable> sendChunks(AbstractFile localFile) throws PermissionDeniedException, IOException {
		
		System.out.printf("Splitting %s into %d chunks%s", localFile.getName(), chunks, System.lineSeparator());
		long started = System.currentTimeMillis();
		ExecutorService executor = Executors.newFixedThreadPool(chunks);
		long chunkLength = localFile.length() / chunks;
		long finalLength = localFile.length() % chunkLength;
		List<ChunkedConsoleProgress> progress = new ArrayList<>();
		List<Throwable> errors = new ArrayList<>();
		for(int i=0;i<chunks;i++) {
			final int chunk = i+1;
			final long pointer = i * chunkLength;
			executor.submit(()-> {
				try {
					ChunkedConsoleProgress tmp = new ChunkedConsoleProgress();
					progress.add(tmp);
					boolean lastChunk = chunk == chunks;
					long thisLength = lastChunk ? chunkLength + finalLength : chunkLength;
					sendChunk(localFile, pointer, thisLength, chunk, lastChunk, tmp);
				} catch (Throwable e) {
					errors.add(e);
				}
			});
			
		}
		executor.shutdown();
		boolean cont = true;
		while(errors.isEmpty() && cont) {
			try {
				cont = !executor.awaitTermination(1, TimeUnit.SECONDS);
				report(progress, localFile.length(), started);
			} catch (InterruptedException e) {
				break;
			}
		}
		
		return errors;
	}

	private void sendFileViaSFTP(AbstractFile localFile, String remotePath) throws IOException, SshException, PermissionDeniedException, SftpStatusException, TransferCancelledException {
		SshClient ssh = clients.removeFirst();
		try(SftpClient sftp = new SftpClient(ssh)) {
			sftp.cd(remoteFolder);
			sftp.put(localFile.getAbsolutePath(), remotePath, new SingleTransferConsoleProgress(localFile.length()));
		} finally {
			clients.addLast(ssh);;
		}
	}

	private void sendFileViaSCP(AbstractFile localPath, String remotePath) throws SshException, ChannelOpenException, IOException, PermissionDeniedException {
		
		SshClient ssh = clients.removeFirst();
		try {
			ScpClient scp = new ScpClient(ssh);
			 scp.putFile(localPath.getAbsolutePath(), remotePath, false, 
				new SingleTransferConsoleProgress(localPath.length()), false);
		} finally {
			clients.addLast(ssh);;
		}
	
	}

	private void verifyIntegrity(AbstractFile localFile) throws SshException, SftpStatusException, IOException, PermissionDeniedException {
		
		if(verifyIntegrity) {
						
			SftpClient sftp = getSftpClient();
			try {
				sftp.cd(remoteFolder);
				System.out.printf("Verifying %s%s", localFile.getName(), System.lineSeparator());
				if(sftp.verifyFiles(localFile.getAbsolutePath(), localFile.getName(), digest)) {
					System.out.printf("The integrity of %s has been verified%s", localFile.getName(), System.lineSeparator());
				} else {
					throw new IOException(String.format("The local and remote files DO NOT match", localFile.getName()));
				}
			} catch(SftpStatusException e) {
				if(e.getStatus() == SftpStatusException.SSH_FX_OP_UNSUPPORTED) {
					if(!ignoreIntegrity) {
						throw new IOException(String.format("The remote server does not support integrity verification"));
					}
					System.out.println(String.format("Ignoring that the remote server does not support integrity verification"));
				} else {
					throw e;
				}
			} 
		}
		
	}

	private void sendChunk(AbstractFile localFile, long pointer, long chunkLength, Integer chunkNumber, boolean lastChunk, FileTransferProgress progress) throws IOException, SftpStatusException, SshException, TransferCancelledException, ChannelOpenException, PermissionDeniedException {
		
		System.out.printf("Starting chunk %d at position %d with length of %d bytes%s", chunkNumber, pointer, chunkLength, System.lineSeparator());
		
		SshClient ssh = clients.removeFirst();
		try (RandomAccessFile file = new RandomAccessFile(localFile.getAbsolutePath(), "r")) {
			file.seek(pointer);
			
			if(forceSFTP) {
				try(SftpClient sftp = new SftpClient(ssh)) {
					sftp.cd(remoteFolder);
					sftp.put(new ChunkInputStream(file, chunkLength), 
							localFile.getName() + "_part" + chunkNumber, progress);
				} 
			} else {
				ScpClientIO scp = new ScpClientIO(ssh);
				scp.put(new ChunkInputStream(file, chunkLength), 
						chunkLength, localFile.getName(), 
						FileUtils.checkEndsWithSlash(remoteFolder) + localFile.getName() + "_part" + chunkNumber, 
						progress);
			}
		} finally {
			clients.addLast(ssh);;
		}
	}
	
	public synchronized void report(List<ChunkedConsoleProgress> progress, long length, long started) {
		
		if(!reportDone) {
			long totalSoFar = 0;
			for(ChunkedConsoleProgress p : progress) {
				totalSoFar += p.getLength();
			}
			
			reportDone = report(totalSoFar, length, started);
		}
	}
		
	
	
	class ChunkedConsoleProgress implements FileTransferProgress {

		AtomicLong length = new AtomicLong(0L);
		ChunkedConsoleProgress() {
		}
		
		public long getLength() {
			return length.get();
		}

		@Override
		public synchronized void progressed(long transferred) {
			this.length.set(transferred);
		}
	}
	
	class SingleTransferConsoleProgress implements FileTransferProgress {

		long started = System.currentTimeMillis();
		long length;
		long lastUpdate;
		int lastOutputLength = -1;
		SingleTransferConsoleProgress(long length) {
			this.length = length;
		}
	
		@Override
		public void progressed(long transferred) {
			report(transferred, length, started);
		}
	
		@Override
		public void completed() {
			progressed(length);
		}
	}
}
