pipeline {
 	agent none
 	tools {
		maven 'Maven 3.9.0' 
		jdk 'Graal JDK 21' 
	}
	
	environment {
	    /* Constants / Configuration */
	    BUILD_PROPERTIES_ID = "b60f3998-d8fd-434b-b3c8-ed52aa52bc2e"
	    BUILD_PROPERTIES_NAME = "jadaptive.build.properties"
	    MAVEN_CONFIG_ID = "14324b85-c597-44e8-a575-61f925dba528"
	}

	stages {
		stage ('PushSFTP and FileDrop Installers') {
			parallel {
				/*
				 * Linux Intel Installers and Packages
				 */
				stage ('Linux Intel PushSFTP and FileDrop Installers') {
					agent {
						label 'install4j && linux && x86_64'
					}
					steps {
						configFileProvider([
					 			configFile(
					 				fileId: "${env.BUILD_PROPERTIES_ID}",  
					 				replaceTokens: true,
					 				targetLocation: "${env.BUILD_PROPERTIES_NAME}",
					 				variable: 'BUILD_PROPERTIES'
					 			)
					 		]) {
					 		withMaven(
					 			globalMavenSettingsConfig: "${env.MAVEN_CONFIG_ID}"
					 		) {
					 		  	sh 'mvn -U -Dbuild.mediaTypes=unixInstaller,unixArchive,linuxRPM,linuxDeb ' +
					 		  	   '-Dbuild.projectProperties=$BUILD_PROPERTIES ' +
					 		  	   '-Dinstall4j.disableSigning=true ' +
					 		  	   '-Dbuild.buildIds=26,37,46,112 ' +
					 		  	   '-Dbuild.gui.buildIds=25,37,116 ' +
					 		  	   '-DbuildInstaller=true clean package'
					 		  	
					 		  	/* Stash installers */
			        			stash includes: 'push-sftp/target/media/*', name: 'linux-cli'
			        			stash includes: 'push-sftp-gui/target/media/*', name: 'linux-gui'
			        			
			        			/* Stash updates.xml */
			        			dir('push-sftp-gui/target/media') {
									stash includes: 'updates.xml', name: 'linux-gui-updates-xml'
			        			}
			        			dir('push-sftp/target/media') {
									stash includes: 'updates.xml', name: 'linux-updates-xml'
			        			}
					 		}
        				}
					}
				}
				/*
				 * Linux Aarch64 Installers and Packages
				 */
				stage ('Linux Aarch64 PushSFTP and FileDrop Installers') {
					agent {
						label 'install4j && linux && aarch64'
					}
					steps {
						configFileProvider([
					 			configFile(
					 				fileId: "${env.BUILD_PROPERTIES_ID}",  
					 				replaceTokens: true,
					 				targetLocation: "${env.BUILD_PROPERTIES_NAME}",
					 				variable: 'BUILD_PROPERTIES'
					 			)
					 		]) {
					 		withMaven(
					 			globalMavenSettingsConfig: "${env.MAVEN_CONFIG_ID}"
					 		) {
					 		  	sh 'mvn -U -Dbuild.mediaTypes=unixInstaller,unixArchive,linuxRPM,linuxDeb ' +
					 		  	   '-Dbuild.projectProperties=$BUILD_PROPERTIES ' +
					 		  	   '-Dinstall4j.disableSigning=true ' +
					 		  	   '-Dbuild.buildIds=118,122,133,137 ' +
					 		  	   '-Dbuild.gui.buildIds=138,145,151 ' +
					 		  	   '-DbuildInstaller=true clean package'
					 		  	
					 		  	/* Stash installers */
			        			stash includes: 'push-sftp/target/media/*', name: 'linux-aarch64-cli'
			        			stash includes: 'push-sftp-gui/target/media/*', name: 'linux-aarch64-gui'
			        			
			        			/* Stash updates.xml */
			        			dir('push-sftp-gui/target/media') {
									stash includes: 'updates.xml', name: 'linux-aarch64-gui-updates-xml'
			        			}
			        			dir('push-sftp/target/media') {
									stash includes: 'updates.xml', name: 'linux-aarch64-updates-xml'
			        			}
					 		}
        				}
					}
				}
				
				/*
				 * Windows installers
				 */
				stage ('Windows PushSFTP and FileDrop Installers') {
					agent {
						label 'install4j && windows'
					}
					steps {
						configFileProvider([
					 			configFile(
					 				fileId: "${env.BUILD_PROPERTIES_ID}",  
					 				replaceTokens: true,
					 				targetLocation: "${env.BUILD_PROPERTIES_NAME}",
					 				variable: 'BUILD_PROPERTIES'
					 			)
					 		]) {
					 		withMaven(
					 			globalMavenSettingsConfig: "${env.MAVEN_CONFIG_ID}"
					 		) {
					 		  	bat 'mvn -U -Dinstall4j.verbose=true -Dbuild.mediaTypes=windows,windowsArchive ' +
					 		  	    '"-Dbuild.projectProperties=%BUILD_PROPERTIES%" ' +
				 		  	        '-DbuildInstaller=true clean package'
					 		  	
					 		  	/* Stash installers */
			        			stash includes: 'push-sftp/target/media/*', name: 'windows-cli'
			        			stash includes: 'push-sftp-gui/target/media/*', name: 'windows-gui'
			        			
			        			/* Stash updates.xml */
			        			dir('push-sftp-gui/target/media') {
									stash includes: 'updates.xml', name: 'windows-gui-updates-xml'
			        			}
			        			dir('push-sftp/target/media') {
									stash includes: 'updates.xml', name: 'windows-updates-xml'
			        			}
					 		}
        				}
					}
				}
				
				/*
				 * MacOS Intel installers
				 */
				stage ('MacOS Intel PushSFTP and FileDrop Installers') {
					agent {
						label 'install4j && macos && x86_64'
					}
					steps {
						configFileProvider([
					 			configFile(
					 				fileId: "${env.BUILD_PROPERTIES_ID}",  
					 				replaceTokens: true,
					 				targetLocation: "${env.BUILD_PROPERTIES_NAME}",
					 				variable: 'BUILD_PROPERTIES'
					 			)
					 		]) {
					 		withMaven(
					 			globalMavenSettingsConfig: "${env.MAVEN_CONFIG_ID}"
					 		) {
					 			// -Dinstall4j.disableNotarization=true 
					 		  	sh 'mvn -X -U -Dbuild.mediaTypes=macos,macosFolder,macosFolderArchive ' +
					 		  	   '-Dbuild.projectProperties=$BUILD_PROPERTIES ' +
					 		  	   '-Dinstall4j.debug=true ' +
					 		  	   '-Dbuild.buildIds=36 ' +
					 		  	   '-Dbuild.gui.buildIds=36 ' +
					 		  	   '-DbuildInstaller=true clean package'
					 		  	
					 		  	/* Stash installers */
			        			stash includes: 'push-sftp/target/media/*', name: 'macos-cli'
			        			stash includes: 'push-sftp-gui/target/media/*', name: 'macos-gui'
			        			
			        			/* Stash updates.xml */
			        			dir('push-sftp-gui/target/media') {
									stash includes: 'updates.xml', name: 'macos-gui-updates-xml'
			        			}
			        			dir('push-sftp/target/media') {
									stash includes: 'updates.xml', name: 'macos-updates-xml'
			        			}
					 		}
        				}
					}
				}
				
				/*
				 * MacOS Aarch64 installers
				 */
				stage ('MacOS Aarch64 PushSFTP and FileDrop Installers') {
					agent {
						label 'install4j && macos && aarch64'
					}
					steps {
						configFileProvider([
					 			configFile(
					 				fileId: "${env.BUILD_PROPERTIES_ID}",  
					 				replaceTokens: true,
					 				targetLocation: "${env.BUILD_PROPERTIES_NAME}",
					 				variable: 'BUILD_PROPERTIES'
					 			)
					 		]) {
					 		withMaven(
					 			globalMavenSettingsConfig: "${env.MAVEN_CONFIG_ID}"
					 		) {
					 			// -Dinstall4j.disableNotarization=true 
					 		  	sh 'mvn -X -U -Dbuild.mediaTypes=macos,macosFolder,macosFolderArchive ' +
					 		  	   '-Dbuild.projectProperties=$BUILD_PROPERTIES ' +
					 		  	   '-Dbuild.buildIds=118 ' +
					 		  	   '-Dbuild.gui.buildIds=128 ' +
					 		  	   '-Dinstall4j.debug=true ' +
					 		  	   '-DbuildInstaller=true clean package'
					 		  	
					 		  	/* Stash installers */
			        			stash includes: 'push-sftp/target/media/*', name: 'macos-aarch64-cli'
			        			stash includes: 'push-sftp-gui/target/media/*', name: 'macos-aarch64-gui'
			        			
			        			/* Stash updates.xml */
			        			dir('push-sftp-gui/target/media') {
									stash includes: 'updates.xml', name: 'macos-aarch64-gui-updates-xml'
			        			}
			        			dir('push-sftp/target/media') {
									stash includes: 'updates.xml', name: 'macos-aarch64-updates-xml'
			        			}
					 		}
        				}
					}
				}
			}
		}
		
		stage ('Deploy') {
			agent {
				label 'linux && !remote'
			}
			steps {
    			/* Clean */
    			withMaven(
		 			globalMavenSettingsConfig: "${env.MAVEN_CONFIG_ID}"
		 		) {
					sh 'mvn clean'
		 		}
			
				script {
					/* Create full version number from Maven POM version and the
					   build number */
					def pom = readMavenPom file: 'pom.xml'
					pom_version_array = pom.version.split('\\.')
					suffix_array = pom_version_array[2].split('-')
					env.FULL_VERSION = pom_version_array[0] + '.' + pom_version_array[1] + "." + suffix_array[0] + "-${BUILD_NUMBER}"
					echo 'Full Maven Version ' + env.FULL_VERSION
				}
				
				/* Unstash installers */
	 		  	unstash 'linux-cli'
	 		  	unstash 'linux-gui'
	 		  	unstash 'linux-aarch64-cli'
	 		  	unstash 'linux-aarch64-gui'
	 		  	unstash 'windows-cli'
	 		  	unstash 'windows-gui'
	 		  	unstash 'macos-cli'
	 		  	unstash 'macos-gui'
	 		  	unstash 'macos-aarch64-cli'
	 		  	unstash 'macos-aarch64-gui'
	 		  	
				/* Unstash updates.xml */
	 		  	dir('push-sftp-gui/target/media-linux') {
	 		  		unstash 'linux-gui-updates-xml'
    			}
    			dir('push-sftp-gui/target/media-linux-aarch64') {
	 		  		unstash 'linux-aarch64-gui-updates-xml'
    			}
	 		  	dir('push-sftp/target/media-linux') {
	 		  		unstash 'linux-updates-xml'
    			}
	 		  	dir('push-sftp/target/media-linux-aarch64') {
	 		  		unstash 'linux-aarch64-updates-xml'
    			}
    			
	 		  	dir('push-sftp-gui/target/media-windows') {
	 		  		unstash 'windows-gui-updates-xml'
    			}
	 		  	dir('push-sftp/target/media-windows') {
	 		  		unstash 'windows-updates-xml'
    			}
    			
	 		  	dir('push-sftp-gui/target/media-macos') {
	 		  		unstash 'macos-gui-updates-xml'
    			}
	 		  	dir('push-sftp/target/media-macos') {
	 		  		unstash 'macos-updates-xml'
    			}
    			dir('push-sftp-gui/target/media-macos-aarch64') {
	 		  		unstash 'macos-aarch64-gui-updates-xml'
    			}
	 		  	dir('push-sftp/target/media-macos-aarch64') {
	 		  		unstash 'macos-aarch64-updates-xml'
    			}
    			
    			/* Merge all updates.xml into one */
    			withMaven(
		 			globalMavenSettingsConfig: "${env.MAVEN_CONFIG_ID}",
		 		) {
					sh 'mvn -P merge-installers -pl push-sftp-gui com.sshtools:updatesxmlmerger-maven-plugin:merge'
					sh 'mvn -P merge-installers -pl push-sftp com.sshtools:updatesxmlmerger-maven-plugin:merge'
		 		}
		 		
    			/* Upload all CLI and GUI installers */
		 		s3Upload(
		 			consoleLogLevel: 'INFO', 
		 			dontSetBuildResultOnFailure: false, 
		 			dontWaitForConcurrentBuildCompletion: false, 
		 			entries: [[
		 				bucket: 'sshtools-public/push-sftp/' + env.FULL_VERSION, 
		 				noUploadOnFailure: true, 
		 				selectedRegion: 'eu-west-1', 
		 				sourceFile: 'push-sftp/target/media/*', 
		 				storageClass: 'STANDARD', 
		 				useServerSideEncryption: false]], 
		 			pluginFailureResultConstraint: 'FAILURE', 
		 			profileName: 'JADAPTIVE Buckets', 
		 			userMetadata: []
		 		)
		 		s3Upload(
		 			consoleLogLevel: 'INFO', 
		 			dontSetBuildResultOnFailure: false, 
		 			dontWaitForConcurrentBuildCompletion: false, 
		 			entries: [[
		 				bucket: 'sshtools-public/push-sftp-gui/' + env.FULL_VERSION, 
		 				noUploadOnFailure: true, 
		 				selectedRegion: 'eu-west-1', 
		 				sourceFile: 'push-sftp-gui/target/media/*', 
		 				storageClass: 'STANDARD', 
		 				useServerSideEncryption: false]], 
		 			pluginFailureResultConstraint: 'FAILURE', 
		 			profileName: 'JADAPTIVE Buckets', 
		 			userMetadata: []
		 		)
		 		
    			/* Copy the merged updates.xml (for FileDrop) to the nightly directory so updates can be seen
    			by anyone on this channel */
		 		s3Upload(
		 			consoleLogLevel: 'INFO', 
		 			dontSetBuildResultOnFailure: false, 
		 			dontWaitForConcurrentBuildCompletion: false, 
		 			entries: [[
		 				bucket: 'sshtools-public/push-sftp/continuous', 
		 				noUploadOnFailure: true, 
		 				selectedRegion: 'eu-west-1', 
		 				sourceFile: 'push-sftp/target/media/updates.xml', 
		 				storageClass: 'STANDARD', 
		 				useServerSideEncryption: false]], 
		 			pluginFailureResultConstraint: 'FAILURE', 
		 			profileName: 'JADAPTIVE Buckets', 
		 			userMetadata: []
		 		)
		 		s3Upload(
		 			consoleLogLevel: 'INFO', 
		 			dontSetBuildResultOnFailure: false, 
		 			dontWaitForConcurrentBuildCompletion: false, 
		 			entries: [[
		 				bucket: 'sshtools-public/push-sftp-gui/continuous', 
		 				noUploadOnFailure: true, 
		 				selectedRegion: 'eu-west-1', 
		 				sourceFile: 'push-sftp-gui/target/media/updates.xml', 
		 				storageClass: 'STANDARD', 
		 				useServerSideEncryption: false]], 
		 			pluginFailureResultConstraint: 'FAILURE', 
		 			profileName: 'JADAPTIVE Buckets', 
		 			userMetadata: []
		 		)
			}					
		}		
	}
}