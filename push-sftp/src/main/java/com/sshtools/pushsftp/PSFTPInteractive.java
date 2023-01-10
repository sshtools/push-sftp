package com.sshtools.pushsftp;

import java.io.IOException;

import com.sshtools.client.SshClient;
import com.sshtools.client.sftp.SftpClient;
import com.sshtools.commands.CliCommand;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.ssh.SshException;

import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "psftp", mixinStandardHelpOptions = true, description = "Push secure file transfer")
public class PSFTPInteractive extends CliCommand {

	SftpClient sftp;

	public SftpClient getSftpClient() {
		return sftp;
	}
	
	protected void onConnected(SshClient ssh) {
		try {
			sftp = new SftpClient(ssh);
		} catch (SshException | PermissionDeniedException | IOException e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
	}

	@Override
	protected boolean canExit() {
		return false;
	}

	@Override
	protected void error(String msg, Exception e) {
		System.err.println(msg);
	}

	@Override
	protected boolean isQuiet() {
		return true;
	}

	@Override
	protected Object createInteractiveCommand() {
		return new PSFTPCommands(this);
	}

	@Override
	protected String getCommandName() {
		return "psftp";
	}
	
	public static void main(String[] args) throws Exception {
		System.exit(new CommandLine(new PSFTPInteractive()).execute(args));
	}

	public SshClient getSshClient() {
		return ssh;
	}

}
