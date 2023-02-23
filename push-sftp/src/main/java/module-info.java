module com.sshtools.pushsftp {
	requires java.prefs;
	requires java.net.http;
	requires com.sshtools.sequins;
	requires info.picocli;
	requires com.sshtools.common.logger;
	requires transitive com.sshtools.synergy.client;
	requires transitive com.sshtools.commands;
	opens com.sshtools.pushsftp;
	exports com.sshtools.pushsftp;
}