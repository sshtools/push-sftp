package com.sshtools.pushsftp.commands;

import java.util.Objects;

import com.sshtools.client.SshClient;
import com.sshtools.common.publickey.SshKeyUtils;

import picocli.CommandLine.Command;

@Command(name = "info", usageHelpAutoWidth = true, mixinStandardHelpOptions = true, description = "Display information on the current connection")
public class Info extends SftpCommand {
	
	@Override
	protected Integer onCall() throws Exception {
		SshClient ssh = getSshClient();
		
		var term = io();
		var connection = ssh.getConnection();
		
		term.messageln(term.createSequence().underlineOn().span("Host", term.getWidth() / 2).toString());
		term.newline();
		term.messageln("Hostname             : {0}", ssh.getHost());
		term.messageln("Port                 : {0}", ssh.getPort());
		term.messageln("Username             : {0}", connection.getUsername());
		term.messageln("Remote identification: {0}", ssh.getRemoteIdentification());
		term.newline();

		term.messageln(term.createSequence().underlineOn().span("KEX", term.getWidth() / 2).toString());
		term.newline();
		term.messageln("Host key type        : {0}", connection.getHostKeyAlgorithm());
		term.messageln("Host key fingerprint : {0}", SshKeyUtils.getFingerprint(ssh.getHostKey()));
		term.messageln("Key exchange         : {0}", connection.getKeyExchangeInUse());
		term.messageln("Cipher (c->s)        : {0}", connection.getCipherInUseCS());
		term.messageln("Cipher (s->c)        : {0}", connection.getCipherInUseSC());
		term.messageln("Mac (c->s)           : {0}", connection.getMacInUseCS());
		term.messageln("Mac (s->c)           : {0}", connection.getMacInUseSC());
		term.messageln("Compression (c->s)   : {0}", connection.getCompressionInUseCS());
		term.messageln("Compression (s->c)   : {0}", connection.getCompressionInUseSC());
		term.newline();
		term.messageln(term.createSequence().underlineOn().span("SFTP", term.getWidth() / 2).toString());
		term.newline();	
		
		if(showSFTPStatistic(      "Write blocksize      : {0} bytes", "maverick.write.optimizedBlock")) {
			showSFTPStatistic(      "Write roundtrip      : {0} ", "maverick.write.blockRoundtrip");
			showSFTPStatistic(      "Write max requests   : {0}", "maverick.write.minAsyncRequests"); 
			showSFTPStatistic(      "Write min requests   : {0}", "maverick.write.maxAsyncRequests"); 
		} else {
			term.messageln("Perform a put or push operation to generate SFTP write operation statistics");
		}
		if(showSFTPStatistic(      "Read blocksize       : {0}ms", "maverick.read.optimizedBlock")) {
			showSFTPStatistic(      "Read roundtrip       : {0}ms", "maverick.read.blockRoundtrip");
			showSFTPStatistic(      "Read async requests  : {0}", "maverick.read.asyncRequests");
		} else {
			term.messageln("Perform a get or pull operation to generate SFTP read operation statistics");
		}
		term.newline();	
	
		return 0;
	}

	private boolean showSFTPStatistic(String message, String value) {
		String v = System.getProperty(value);
		if(Objects.isNull(v)) {
			return false;
		}
		
		io().messageln(message, v);
		return true;
	}
}
