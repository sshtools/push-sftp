package com.sshtools.pushsftp.commands;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Optional;

import com.sshtools.client.SshClient;
import com.sshtools.client.sftp.SftpClient;
import com.sshtools.client.sftp.SftpFile;
import com.sshtools.client.sftp.SftpFileVisitor;
import com.sshtools.client.tasks.FileTransferProgress;
import com.sshtools.commands.ChildCommand;
import com.sshtools.common.sftp.SftpStatusException;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.util.FileUtils;
import com.sshtools.common.util.IOUtils;
import com.sshtools.pushsftp.PSFTPCommands;
import com.sshtools.pushsftp.PSFTPInteractive;
import com.sshtools.sequins.Progress;
import com.sshtools.sequins.Progress.Level;
import com.sshtools.sequins.ProgressBar;
import com.sshtools.sequins.Terminal;

public abstract class SftpCommand extends ChildCommand {

	public interface FileOp {
		void op(String path) throws Exception;
	}

	protected Terminal getTerminal() {
		return ((PSFTPInteractive)getRootCommand()).getTerminal();
	}

	protected SftpClient getSftpClient() {
		return ((PSFTPInteractive)getRootCommand()).getSftpClient();
	}

	protected SshClient getSshClient() {
		return ((PSFTPInteractive)getRootCommand()).getSshClient();
	}

	protected PSFTPCommands getInteractiveCommand() {
		return (PSFTPCommands)getSpec().parent().userObject();
	}

	protected String getUsername() {
		return getRootCommand().getUsername();
	}

	protected String getHost() {
		return getRootCommand().getHost();
	}

	protected int getPort() {
		return getRootCommand().getPort();
	}

	protected void expand(String path, FileOp op, boolean recurse) throws SshException, SftpStatusException {
		PathMatcher matcher =
			    FileSystems.getDefault().getPathMatcher("glob:" + path);

		getSftpClient().visit(path, new SftpFileVisitor() {
			@Override
			public FileVisitResult postVisitDirectory(SftpFile dir, IOException exc) throws IOException {
				if(recurse)
					return FileVisitResult.TERMINATE;
				else
					return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFile(SftpFile file, BasicFileAttributes attrs) throws IOException {
				String filePath = file.getAbsolutePath();
				if(matcher.matches(Paths.get(filePath))) {
					try {
						op.op(filePath);
					} catch(IOException | RuntimeException re) {
						throw re;
					} catch (Exception e) {
						throw new IOException("Failed to match pattern.", e);
					}
				}
				return FileVisitResult.CONTINUE;
			}
		});
	}

	protected int getUID(String username) throws IOException {

		SshClient ssh = getSshClient();
		try {
			return Integer.parseInt(ssh.executeCommand(String.format("id -u %s", username)));
		} catch (NumberFormatException e) {
			throw new IOException("Could not determine uid from username");
		}
	}

	protected int getGID(String groupname) throws IOException {

		SshClient ssh = getSshClient();
		try {
			return Integer.parseInt(ssh.executeCommand(String.format("id -u %s", groupname)));
		} catch (NumberFormatException e) {
			throw new IOException("Could not determine uid from username");
		}
	}

	public synchronized boolean report(Progress progress, String name, long totalSoFar, long length, long started) {

		boolean isDone = false;
		if(totalSoFar > 0) {

			String state = "ETA";
			if(totalSoFar >= length) {
				state = "DONE";
				isDone = true;
			}

			var cols = getRootCommand().getTerminal().getWidth();

			var percentage = ((double) totalSoFar / (double)length) * 100;
			var percentageStr = String.format("%.0f%%", percentage);

			var humanBytes = IOUtils.toByteSize(totalSoFar);

			var time = (System.currentTimeMillis() - started);

			var megabytesPerSecond = (totalSoFar / time) / 1024D;
			var transferRate = String.format("%.1fMB/s", megabytesPerSecond);

			var remaining = (length - totalSoFar);
			var perSecond = (long) (megabytesPerSecond * 1024);
			var seconds = (remaining / perSecond) / 1000l;

			var available = cols - 41;

			var half = available / 2;
			var nameLen = half;
			var progressLen = 0;
			if(nameLen > 10) {
				if(nameLen + progressLen != available) {
					nameLen --;
				}
				progressLen = half;
			}

			var pb = new ProgressBar<>(getTerminal());
			pb.setMax(length);
			pb.setValue(totalSoFar);


			if(name.length() > nameLen) {
				name = "..." + name.substring(0, Math.max(3, nameLen) - 3);
			}

			var output = String.format("%-" + nameLen + "s %s%4s %8s %10s %5d:%02d %-4s",
					name, pb.drawToString(progressLen) + " ", percentageStr, humanBytes, transferRate,
					(int) (seconds > 60 ? seconds / 60 : 0),
					(int) (seconds % 60),
					state);

			if(isDone)
				progress.message(Level.NORMAL, output, Optional.of(percentage));
			else
				progress.progressed(Optional.of((int)percentage), Optional.of(output));
		}
		return isDone;
	}

	protected FileTransferProgress fileTransferProgress(Progress progress, String messagePattern) {
		return new FileTransferProgress() {
			private long bytesTotal;
			private long started;
			private String file;

			@Override
			public void started(long bytesTotal, String file) {
				this.started = System.currentTimeMillis();
				this.bytesTotal = bytesTotal;
				this.file = FileUtils.getFilename(file);
				progress.message(Level.NORMAL, messagePattern, this.file);
			}

			@Override
			public boolean isCancelled() {
				return progress.isCancelled();
			}

			@Override
			public void progressed(long bytesSoFar) {
				report(progress, file, bytesSoFar, bytesTotal, started);
			}
		};
	}
}
