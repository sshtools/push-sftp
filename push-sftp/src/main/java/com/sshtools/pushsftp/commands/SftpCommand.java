package com.sshtools.pushsftp.commands;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Objects;

import com.sshtools.client.SshClient;
import com.sshtools.client.sftp.SftpClient;
import com.sshtools.client.sftp.SftpFile;
import com.sshtools.client.sftp.SftpFileVisitor;
import com.sshtools.commands.ChildCommand;
import com.sshtools.common.sftp.SftpStatusException;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.util.IOUtils;
import com.sshtools.pushsftp.PSFTPCommands;

public abstract class SftpCommand extends ChildCommand {

	public interface FileOp {
		void op(String path) throws Exception;
	}
	
	protected SftpClient getSftpClient() {
		return getInteractiveCommand().getParentCommand().getSftpClient();
	}
	
	protected SshClient getSshClient() {
		return getInteractiveCommand().getParentCommand().getSshClient();
	}

	protected PSFTPCommands getInteractiveCommand() {
		return (PSFTPCommands)getSpec().parent().userObject();
	}
	
	protected String getUsername() {
		return getInteractiveCommand().getParentCommand().getUsername();
	}
	
	protected String getHost() {
		return getInteractiveCommand().getParentCommand().getHost();
	}
	
	protected int getPort() {
		return getInteractiveCommand().getParentCommand().getPort();
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
	
	public synchronized boolean report(long totalSoFar, long length, long started) {
		
		boolean isDone = false;
		if(totalSoFar > 0) {
			
			String state = "ETA";
			if(totalSoFar >= length) {
				state = "DONE";
				isDone = true;
			}
			
			Double percentage = ((double) totalSoFar / (double)length) * 100;
			String percentageStr = String.format("%.0f%%", percentage);
			
			String humanBytes = IOUtils.toByteSize(totalSoFar);
			
			long time = (System.currentTimeMillis() - started);
		
			Double megabytesPerSecond = (totalSoFar / time) / 1024D;
			String transferRate = String.format("%.1fMB/s", megabytesPerSecond);
			
			long remaining = (length - totalSoFar);
			long perSecond = (long) (megabytesPerSecond * 1024);
			long seconds = (remaining / perSecond) / 1000;
			
			String output = String.format("%4s %8s %10s %02d:%02d %4s ", 
					percentageStr, humanBytes, transferRate,
					(int) (seconds > 60 ? seconds / 60 : 0), 
					(int) (seconds % 60), 
					state);
			
			System.out.print("\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b");
			
			System.out.print(output);
			if(Objects.isNull(System.console()) || isDone) {
				System.out.println();
			}
		}
		return isDone;
	}
}
