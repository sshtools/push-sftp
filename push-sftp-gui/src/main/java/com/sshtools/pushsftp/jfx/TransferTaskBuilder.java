package com.sshtools.pushsftp.jfx;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.sshtools.client.PasswordAuthenticator.PasswordPrompt;

public interface TransferTaskBuilder<BLDR extends TransferTaskBuilder<BLDR, TARG, TSK>, TARG extends Target, TSK extends TargetJob<?, TARG>> {
	BLDR withVerbose(boolean verbose);
	BLDR withPaths(List<Path> files);
	BLDR withReporter(Reporter reporter);
	BLDR withPassword(PasswordPrompt password);
	BLDR withTarget(Supplier<TARG> target);
	BLDR withSerializer(Consumer<TARG> serializer);
	
	TSK build();
}
