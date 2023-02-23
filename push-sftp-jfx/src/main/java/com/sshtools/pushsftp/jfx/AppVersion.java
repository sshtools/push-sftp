package com.sshtools.pushsftp.jfx;

import com.sshtools.sequins.ArtifactVersion;

import picocli.CommandLine.IVersionProvider;

final class AppVersion implements IVersionProvider {
	@Override
	public String[] getVersion() throws Exception {
		return ArtifactVersion.getVersion("com.sshtools", "key-management-jfx").split("\\.|-");
	}
}