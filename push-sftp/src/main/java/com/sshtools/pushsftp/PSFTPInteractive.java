package com.sshtools.pushsftp;

import java.io.IOException;
import java.util.Optional;

import com.sshtools.client.SshClient;
import com.sshtools.client.sftp.SftpClient;
import com.sshtools.commands.CliCommand;
import com.sshtools.commands.ExceptionHandler;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.sftp.SftpStatusException;
import com.sshtools.common.ssh.SshException;
import com.sshtools.sequins.ArtifactVersion;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.IVersionProvider;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "push-sftp-interactive", description = "Push secure file transfer", versionProvider = PSFTPInteractive.Version.class)
public class PSFTPInteractive extends CliCommand {
	
	public final static class Version implements IVersionProvider {

		@Override
		public String[] getVersion() throws Exception {
			return new String[] { ArtifactVersion.getVersion("com.sshtools", "push-sftp") };
		}
		
	}

	SftpClient sftp;

	@Option(names = { "-h", "--host" }, paramLabel = "HOSTNAME", description = "the hostname of the SSH server. Either this must be supplied or the username can be supplied in the destination")
    Optional<String> host;

	@Option(names = { "--help" }, usageHelp = true)
    boolean help;

	@Option(names = { "-v", "--version" }, versionHelp = true)
    boolean version;

	@Option(names = { "-p", "--port" }, paramLabel = "PORT", description = "the port of the SSH server")
    int port = 22;

	@Option(names = { "-u", "--user" }, paramLabel = "USER", description = "the username to authenticate with. Either this must be supplied or the username can be supplied in the destination")
    Optional<String> username;
	
	@Parameters(index = "0", arity = "0..1", description = "The remote server, with optional username.")
	private Optional<String> destination;

	public SftpClient getSftpClient() {
		return sftp;
	}

	@Override
	protected void onConnected(SshClient ssh) {
		try {
			sftp = new SftpClient(ssh);
			sftp.lcd(getLcwd().getAbsolutePath());
		} catch (SshException | PermissionDeniedException | IOException | SftpStatusException e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
	}

	@Override
	public String getHost() {
		if(destination.isPresent()) {
			var dst = destination.get();
			var idx = dst.indexOf('@');
			return idx == -1 ? dst : dst.substring(idx  +1);
		}
		return host.orElseThrow(() -> new IllegalArgumentException("Host must be supplied either as an option or as part of the destination."));
	}

	@Override
	public String getUsername() {
		if(destination.isPresent()) {
			var dst = destination.get();
			var idx = dst.indexOf('@');
			if(idx > -1)
				return dst.substring(0, idx);
		}
		return username.orElseThrow(() -> new IllegalArgumentException("Username must be supplied either as an option or as part of the destination."));
	}

	@Override
	protected void beforeCommand() { 
		if(getHost().equals(""))
			throw new IllegalArgumentException("Host name must be supplied.");	
	}

	@Override
	protected boolean canExit() {
		return false;
	}

	@Override
	protected void error(String msg, Exception e) {
		getTerminal().error(msg, e);
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
		return "push-sftp";
	}
	
	public static void main(String[] args) throws Exception {
		var cmd = new PSFTPInteractive();
		System.exit(new CommandLine(cmd).setExecutionExceptionHandler(new ExceptionHandler(cmd)).execute(args));
	}

	public SshClient getSshClient() {
		return ssh;
	}

}
