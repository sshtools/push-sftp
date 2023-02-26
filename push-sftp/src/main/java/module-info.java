module com.sshtools.pushsftp {
	requires java.prefs;
	requires java.net.http;
	requires com.sshtools.sequins;
	requires info.picocli;
	requires com.sshtools.common.logger;
	requires transitive com.sshtools.synergy.client;
	requires transitive com.sshtools.commands;
	requires com.install4j.runtime;
	opens com.sshtools.pushsftp;
	opens com.sshtools.pushsftp.commands;
	exports com.sshtools.pushsftp;
}