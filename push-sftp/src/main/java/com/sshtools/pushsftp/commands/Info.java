package com.sshtools.pushsftp.commands;

import com.sshtools.client.SshClient;
import com.sshtools.common.publickey.SshKeyUtils;

import picocli.CommandLine.Command;

@Command(name = "info", usageHelpAutoWidth = true, mixinStandardHelpOptions = true, description = "Display information on the current connection")
public class Info extends SftpCommand {
	
	@Override
	protected Integer onCall() throws Exception {
		SshClient ssh = getSshClient();
		getTerminal().messageln("Hostname             : {0}", ssh.getHost());
		getTerminal().messageln("Port                 : {0}", ssh.getPort());
		getTerminal().messageln("Username             : {0}", ssh.getConnection().getUsername());
		getTerminal().messageln("Remote identification: {0}", ssh.getRemoteIdentification());
		getTerminal().messageln("Host key type        : {0}", ssh.getConnection().getHostKeyAlgorithm());
		getTerminal().messageln("Host key fingerprint : {0}", SshKeyUtils.getFingerprint(ssh.getHostKey()));
		getTerminal().messageln("Key exchange         : {0}", SshKeyUtils.getFingerprint(ssh.getConnection().getKeyExchangeInUse()));
		getTerminal().messageln("Cipher (c->s)        : {0}", ssh.getConnection().getCipherInUseCS());
		getTerminal().messageln("Cipher (s->c)        : {0}", ssh.getConnection().getCipherInUseSC());
		getTerminal().messageln("Mac (c->s)           : {0}", ssh.getConnection().getMacInUseCS());
		getTerminal().messageln("Mac (s->c)           : {0}", ssh.getConnection().getMacInUseSC());
		getTerminal().messageln("Compression (c->s)   : {0}", ssh.getConnection().getCompressionInUseCS());
		getTerminal().messageln("Compression (s->c)   : {0}", ssh.getConnection().getCompressionInUseSC());
		
		
		getTerminal().messageln("Remote identification: {0}", ssh.getRemoteIdentification());
		
		return 0;
	}
}
