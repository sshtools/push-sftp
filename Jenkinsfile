pipeline {
 	agent none
 	tools {
		maven 'Maven 3.9.0' 
		jdk 'Graal JDK 17' 
	}

	stages {
		stage ('PushSFTP and FileDrop Installers') {
			parallel {
				/*
				 * Linux Installers and Packages
				 */
				stage ('Linux PushSFTP and FileDrop Installers') {
					agent {
						label 'install4j && linux'
					}
					steps {
						configFileProvider([
					 			configFile(
					 				fileId: 'bb62be43-6246-4ab5-9d7a-e1f35e056d69',  
					 				replaceTokens: true,
					 				targetLocation: 'hypersocket.build.properties',
					 				variable: 'BUILD_PROPERTIES'
					 			)
					 		]) {
					 		withMaven(
					 			globalMavenSettingsConfig: '4bc608a8-6e52-4765-bd72-4763f45bfbde'
					 		) {
					 		  	sh 'mvn -Dbuild.mediaTypes=unixInstaller,unixArchive,linuxRPM,linuxDeb ' +
					 		  	   '-Dbuild.projectProperties=$BUILD_PROPERTIES ' +
					 		  	   '-DbuildInstaller=true clean package'
					 		  	
					 		  	/* Stash installers */
			        			stash includes: 'push-sftp/target/media/*', name: 'linux-cli'
			        			stash includes: 'push-sftp-gui/target/media/*', name: 'linux-gui'
			        			
			        			/* Stash updates.xml (just FileDrop atm) */
			        			dir('push-sftp-gui/target/media') {
									stash includes: 'updates.xml', name: 'linux-gui-updates-xml'
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
					 				fileId: 'bb62be43-6246-4ab5-9d7a-e1f35e056d69',  
					 				replaceTokens: true,
					 				targetLocation: 'hypersocket.build.properties',
					 				variable: 'BUILD_PROPERTIES'
					 			)
					 		]) {
					 		withMaven(
					 			globalMavenSettingsConfig: '4bc608a8-6e52-4765-bd72-4763f45bfbde'
					 		) {
					 		  	bat 'mvn -Dinstall4j.verbose=true -Dbuild.mediaTypes=windows,windowsArchive ' +
					 		  	    '"-Dbuild.projectProperties=%BUILD_PROPERTIES%" ' +
				 		  	        '-DbuildInstaller=true clean package'
					 		  	
					 		  	/* Stash installers */
			        			stash includes: 'push-sftp/target/media/*', name: 'windows-cli'
			        			stash includes: 'push-sftp-gui/target/media/*', name: 'windows-gui'
			        			
			        			/* Stash updates.xml (just FileDrop atm) */
			        			dir('push-sftp-gui/target/media') {
									stash includes: 'updates.xml', name: 'windows-gui-updates-xml'
			        			}
					 		}
        				}
					}
				}
				
				/*
				 * MacOS installers
				 */
				stage ('MacOS PushSFTP and FileDrop Installers') {
					agent {
						label 'install4j && macos'
					}
					steps {
						configFileProvider([
					 			configFile(
					 				fileId: 'bb62be43-6246-4ab5-9d7a-e1f35e056d69',  
					 				replaceTokens: true,
					 				targetLocation: 'hypersocket.build.properties',
					 				variable: 'BUILD_PROPERTIES'
					 			)
					 		]) {
					 		withMaven(
					 			globalMavenSettingsConfig: '4bc608a8-6e52-4765-bd72-4763f45bfbde'
					 		) {
					 			// -Dinstall4j.disableNotarization=true 
					 		  	sh 'mvn -Dbuild.mediaTypes=macos,macosFolder,macosFolderArchive ' +
					 		  	   '-Dbuild.projectProperties=$BUILD_PROPERTIES ' +
					 		  	   '-DbuildInstaller=true clean package'
					 		  	
					 		  	/* Stash installers */
			        			stash includes: 'push-sftp/target/media/*', name: 'macos-cli'
			        			stash includes: 'push-sftp-gui/target/media/*', name: 'macos-gui'
			        			
			        			/* Stash updates.xml (just FileDrop atm) */
			        			dir('push-sftp-gui/target/media') {
									stash includes: 'updates.xml', name: 'macos-gui-updates-xml'
			        			}
					 		}
        				}
					}
				}
			}
		}
		
		stage ('Deploy') {
			agent {
				label 'linux'
			}
			steps {
			
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
	 		  	unstash 'windows-cli'
	 		  	unstash 'windows-gui'
	 		  	unstash 'macos-cli'
	 		  	unstash 'macos-gui'
	 		  	
				/* Unstash updates.xml */
	 		  	dir('push-sftp-gui/target/linux-macos') {
	 		  		unstash 'linux-updates-xml'
    			}
	 		  	dir('push-sftp-gui/target/windows-macos') {
	 		  		unstash 'windows-updates-xml'
    			}
	 		  	dir('push-sftp-gui/target/media-macos') {
	 		  		unstash 'macos-updates-xml'
    			}
    			
    			/* Merge all updates.xml into one */
    			withMaven(
		 			globalMavenSettingsConfig: '4bc608a8-6e52-4765-bd72-4763f45bfbde',
		 		) {
					sh 'mvn -P merge-installers -pl push-sft-gui com.sshtools:updatesxmlmerger-maven-plugin:merge'
		 		}
		 		
    			/* Upload all CLI and GUI installers */
		 		s3Upload(
		 			consoleLogLevel: 'INFO', 
		 			dontSetBuildResultOnFailure: false, 
		 			dontWaitForConcurrentBuildCompletion: false, 
		 			entries: [[
		 				bucket: 'sshtools-public/push-sftp/EXPERIMENTAL/' + env.FULL_VERSION, 
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
		 				bucket: 'sshtools-public/push-sftp-gui/EXPERIMENTAL/' + env.FULL_VERSION, 
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
		 				bucket: 'sshtools-public/push-sftp/EXPERIMENTAL/continuous', 
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