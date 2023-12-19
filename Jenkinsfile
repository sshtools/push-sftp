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
			        			
			        			/* Stash updates.xml */
			        			dir('push-sftp/target/media/*') {
									stash includes: 'updates.xml', name: 'linux-cli-updates-xml'
			        			}
			        			dir('push-sftp-gui/target/media/*') {
									stash includes: 'updates.xml', name: 'linux-gui-updates-xml'
			        			}
					 		}
        				}
					}
				}
			}
		}
	}
}