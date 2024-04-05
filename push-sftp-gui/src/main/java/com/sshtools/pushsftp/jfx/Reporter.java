package com.sshtools.pushsftp.jfx;

@FunctionalInterface
public interface Reporter {
	void report(TargetJob<?, ?> job, long length, long totalSoFar, long time);
}