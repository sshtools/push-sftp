package com.sshtools.pushsftp.commands;

import java.io.EOFException;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
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
	
	public interface PathOp {
		void op(Path path) throws Exception;
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

	protected Path expandLocalSingle(Optional<Path> path) throws IOException {
		return expandLocalSingleOr(path).orElseGet(() -> {
			PSFTPInteractive cmd = getInteractiveCommand().rootCommand();
			return cmd.getLcwd();
		});
	}
	
	protected Optional<Path> expandLocalSingleOr(Optional<Path> path) throws IOException {
		if(path.isEmpty()) {
			return path;
		}
		else
			return Optional.of(expandLocalSingle(path.get()));
	}
	
	protected Path expandLocalSingle(Path path) throws IOException {
		var l = new ArrayList<Path>();
		expandLocalAndDo((fp) -> {
			if(!l.isEmpty())
				throw new EOFException();
			l.add(fp);
		}, true, path);
		if(l.isEmpty())
			return path;
		else
			return l.get(0);
	}
	
	protected Path[] expandLocalArray(Path[] paths) throws IOException {
		var l = new ArrayList<Path>();
		for(var path : paths) {
			expandLocalAndDo((fp) -> {
				l.add(fp);
			}, true, path);
		}
		return l.toArray(new Path[0]);
	}

	protected Optional<String> expandRemoteSingleOr(Optional<String> path) throws SshException, SftpStatusException {
		if(path.isEmpty()) {
			return path;
		}
		else
			return Optional.of(expandRemoteSingle(path.get()));
	}

	protected String expandRemoteSingle(Optional<String> path) throws SshException, SftpStatusException {
		var expandRemoteSingleOr = expandRemoteSingleOr(path);
		if(expandRemoteSingleOr.isPresent())
			return expandRemoteSingleOr.get();
		else {
			PSFTPInteractive cmd = getInteractiveCommand().rootCommand();
			return cmd.getSftpClient().pwd();
		}
	}

	protected String expandRemoteSingle(String path) throws SshException, SftpStatusException {
		var l = new ArrayList<String>();
		expandRemoteAndDo((fp) -> {
			if(!l.isEmpty())
				throw new EOFException();
			l.add(fp);
		}, true, path);
		if(l.isEmpty())
			return path;
		else
			return l.get(0);
	}
	
	protected String[] expandRemoteArray(String[] paths)  throws SshException, SftpStatusException {
		var l = new ArrayList<String>();
		for(var path : paths) {
			expandRemoteAndDo((fp) -> {
				l.add(fp);
			}, true, path);
		}
		return l.toArray(new String[0]);
	}

	protected void expandLocalAndDo(PathOp op, boolean recurse, Path... paths) throws IOException  {

		PSFTPInteractive cmd = getInteractiveCommand().rootCommand();
		for(var path : paths) {
			path = expandSpecialLocalPath(path);

			var root = path.isAbsolute() ? path.getRoot() : cmd.getLcwd();
			Path resolved = root;
			var pathCount = path.getNameCount();
			for(int i = 0 ; i < pathCount; i++) {
				var pathPart = path.getName(i);
				if(pathPart.toString().equals("..")) {
					//
					//XXXXXXX TODO XXXXXXXXX
					//
				}
				var matches = 0;
				try(var stream = Files.newDirectoryStream(resolved)) {
					var matcher = FileSystems.getDefault().getPathMatcher("glob:" + pathPart.toString());
					for(var pathPartPath : stream) {
						if(matcher.matches(pathPartPath.getFileName())) {
							resolved = resolved.resolve(pathPartPath.getFileName());
							matches++;
							if(i == pathCount -1) {
								try {
									op.op(path.isAbsolute() ? pathPartPath.normalize() : root.relativize(pathPartPath).normalize());
								} catch(EOFException ee) {
									return;
								} catch(IOException | RuntimeException re) {
									throw re;
								} catch (Exception e) {
									throw new IOException("Failed to match pattern.", e);
								}
							}
						}
					}
				}
				if(!recurse || matches == 0)
					break;
			}
		}
	}

	protected void expandRemoteAndDo(FileOp op, boolean recurse, String... paths) throws SshException, SftpStatusException {
		for(var path : paths) {

			//
			//XXXXXXX TODO XXXXXXXXX
			//
			// Copy the local version (more efficient)
			//
			
			var matcher = FileSystems.getDefault().getPathMatcher("glob:" + path);
			var parent = Paths.get(path).getParent();
			var parentMatcher = parent == null ? null : FileSystems.getDefault().getPathMatcher("glob:" + parent);
	
			getSftpClient().visit(path, new SftpFileVisitor() {
				@Override
				public FileVisitResult preVisitDirectory(SftpFile dir, BasicFileAttributes attrs) throws IOException {
					String filePath = dir.getAbsolutePath();
					if(parentMatcher == null || parentMatcher.matches(Paths.get(filePath))) {
						return FileVisitResult.CONTINUE;
					}
					return FileVisitResult.SKIP_SUBTREE;
				}
				
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
						} catch(EOFException ee) {
							return FileVisitResult.TERMINATE;
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


	public static synchronized boolean report(Terminal terminal, Progress progress, String name, long totalSoFar, long length, long started) {

		boolean isDone = false;
		if(totalSoFar > 0) {

			String state = "ETA";

			var cols = terminal.getWidth();

			var percentage = ((double) totalSoFar / (double)length) * 100;
			var percentageStr = String.format("%.0f%%", percentage);

			var humanBytes = IOUtils.toByteSize(totalSoFar);

			var time = (System.currentTimeMillis() - started);

			var megabytesPerSecond = (totalSoFar / time) / 1024D;
			var transferRate = String.format("%.1fMB/s", megabytesPerSecond);

			var remaining = (length - totalSoFar);
			var perSecond = (long) (megabytesPerSecond * 1024);
			var seconds = (remaining / Math.max(1, perSecond)) / 1000l;
			
			var timeSeconds = seconds;
			
			if(totalSoFar >= length) {
				state = "DONE";
				isDone = true;
				timeSeconds = ( System.currentTimeMillis() - started ) / 1000l;
			}
			
			var timeStr = String.format("%5d:%02d", (int) (timeSeconds > 60 ? timeSeconds / 60 : 0),
					(int) (timeSeconds % 60));

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

			var pb = new ProgressBar<>(terminal);
			pb.setMax(length);
			pb.setValue(totalSoFar);


			if(name.length() > nameLen) {
				name = "..." + name.substring(0, Math.max(3, nameLen) - 3);
			}

			var output = String.format("%-" + nameLen + "s %s%4s %8s %10s %8s %-4s",
					name, pb.drawToString(progressLen) + " ", percentageStr, humanBytes, transferRate,
					timeStr,
					state);
			progress.progressed(Optional.of((int)percentage), Optional.of(output));
		}
		return isDone;
	}

	public static FileTransferProgress fileTransferProgress(Terminal terminal, Progress progress, String messagePattern) {
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
				report(terminal, progress, file, bytesSoFar, bytesTotal, started);
			}
		};
	}

	
//	protected Path resolveLocalPath(Path p) {
//		var expanded = expandPath(p);
//		Path resolved;
//		try {
//			resolved = Paths.get(getSftpClient().lpwd());
//		} catch (IOException | PermissionDeniedException e) {
//			throw new IllegalArgumentException("Could not expand path.");
//		}
//		return resolved.resolve(expanded);
//	}
//	
	static Path expandSpecialLocalPath(Path path) {
		if(path.toString().startsWith("~/") || path.toString().startsWith("~\\")) {
			return Paths.get(System.getProperty("user.home") + path.toString().substring(1));
		}
		else
			return path;
	}
}
