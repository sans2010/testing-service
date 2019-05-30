#!/usr/bin/env groovy

import groovy.json.JsonSlurper
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.FileReader;
import java.io.IOException;

pipeline {
    agent any
	environment {
		def appVersion = ''
		def appGroupId = ''
		def appArtifactId = ''
		def devUrl = 'testing-bcbs-app.cfapps.io'
		def testUrl = 'testing-bcbsTest-app.cfapps.io'
    }
	parameters {
		choice(choices:'deploy-to-dev\ndeploy-proxy-dev\ndeploy-kvm-dev\ndeploy-to-test',description:'Which Env',name:'ENV_DEPLOY')
		choice(choices:'Yes\nNo',description:'Deploy to Test env?',name:'DEPLOY_TO_TEST')
		//string(name:'ARTIFACT_VERSION',defaultValue:'',description:'Enter Artifact version from Artifactory.')
		string(name:'ARTIFACT_ID',defaultValue:'',description:'Enter ARTIFACT ID (same as repo name) to build.')
		//string(name:'GROUP_ID',defaultValue:'',description:'Enter GROUP ID to build.')
	}
	
    stages {
		//stage('SampleJob - Checkout') {
			//when { expression { params.ENV_DEPLOY == 'all' } }
            //steps {
                //echo 'Fetching App from Git repo'
					//checkout([$class: 'GitSCM', branches: [[name: '*/master']], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'git-repo', url: '${gitCommon}/'+params.GIT_REPO_NAME+'.git']]])
            //}
       // }
       stage('Initialize and Validate') {
            steps {
                echo 'Initializing & Validating'
				script {
					MavenXpp3Reader reader = new MavenXpp3Reader();
			        Model model = reader.read(new FileReader(WORKSPACE+"/pom.xml"));
			        println "id = " + model.getId()
			        println "GroupId = " + model.getGroupId()
			        println "ArtifactId = " + model.getArtifactId()
			        println "Version = " + model.getVersion()
					//update next build number
					//def releaseVersion = "test-1"
					appVersion = model.getVersion()
					appGroupId = model.getGroupId()
					appArtifactId = model.getArtifactId()
					
					def releaseVersion = model.getVersion()
					reader = null
					model = null
					def prvBuildDispName = ""
					if(currentBuild.rawBuild.previousBuild != null) {
						prvBuildDispName = currentBuild.rawBuild.previousBuild.displayName
					}
					if (params.ENV_DEPLOY == "deploy-to-test") {
					      releaseVersion = ARTIFACT_VERSION
					}

					echo 'Previous Build Display Name:' + prvBuildDispName
					def nxtBuildNum = updateNextBuildNumber(prvBuildDispName, releaseVersion)
					echo "Current build display name set to -> " + nxtBuildNum

					currentBuild.displayName = nxtBuildNum

					//currentBuild.description = 'Build for version ' + currentBuild.displayName
					echo 'Setting Build #' + currentBuild.displayName
				}
            }
        }
        stage ('SampleJob - Build') {
        agent any
        when { expression { params.ENV_DEPLOY == 'deploy-to-dev' } }
            steps {
                echo 'Building app...'
				script {
					mvnHome = tool 'maven'					
					if(isUnix()) {
						sh "mvn clean install -DskipTests " 
					} else { 
						bat "${mvnHome}/bin/mvn clean install -DskipTests " 
					} 
				}
            }
        }
        stage ('SampleJob - Unit Tests') {
        agent any
        when { expression { params.ENV_DEPLOY == 'deploy-to-dev' } }
            steps {
                echo 'Building app...'
				script {
					mvnHome = tool 'maven'
					//withMaven(maven: 'maven') { 
						if(isUnix()) {
							//sh "mvn clean generate-resources -DskipTests -Djacoco.skip=false -Djacoco.skip.report=false "
							sh "mvn clean test -Djacoco.skip=false -Djacoco.skip.report=false " 
						} else { 
							//bat "mvn clean generate-resources -DskipTests -Djacoco.skip=false -Djacoco.skip.report=false "
							//bat "${mvnHome}/bin/mvn test -Djacoco.skip=false -Djacoco.skip.report=false "  
							bat "${mvnHome}/bin/mvn clean test -Djacoco.skip=false -Djacoco.skip.report=false "  
							jacoco()
						}
						println "WORKSPACE = " + WORKSPACE
						//junit '$WORKSPACE/target/surefire-reports/*.xml' 
					//}
					println "WORKSPACE = " + WORKSPACE
					//junit '$WORKSPACE/target/surefire-reports/*.xml'
				}
            }
        }
		stage('Upload to Artifactory') {
		agent any
		when { expression { params.ENV_DEPLOY == 'deploy-to-dev' } }
			steps {
				echo 'Building app...'
				script{
					def artifactory_server = Artifactory.server "Artifactory"
					def buildInfo = Artifactory.newBuildInfo()
					buildInfo.env.capture = true
					def rtMaven = Artifactory.newMavenBuild();
					rtMaven.tool = "maven"
					
					  rtMaven.deployer releaseRepo:'libs-release-local', snapshotRepo:'libs-snapshot-local', server: artifactory_server
					  rtMaven.resolver releaseRepo:'libs-release', snapshotRepo:'libs-snapshot', server: artifactory_server

					  rtMaven.run pom: 'pom.xml', goals: 'clean install -DskipTests', buildInfo: buildInfo

					  
					  artifactory_server.publishBuildInfo buildInfo
					  
					  
					//junit '/target/surefire-reports/*.xml'
				}
			
			}
		}
		stage ('Uploading to PCF') {
		agent any
		when { expression { params.ENV_DEPLOY == 'deploy-to-dev' } }
            steps {
                echo 'Uploading app...'
				script {
					pushToCloudFoundry cloudSpace: 'bcbsma', credentialsId: 'pcf-cre', organization: 'Northeast / Canada', target: 'https://api.run.pivotal.io'
				}
            }
        }
        stage ('Test Promotion') {
		agent any
		when { expression { params.DEPLOY_TO_TEST == 'Yes' && params.ENV_DEPLOY == 'deploy-to-dev' } }
            steps {
                echo 'Deploying app...'
				script {
					mvnHome = tool 'maven'
					//pushToCloudFoundry cloudSpace: 'bcbsma', credentialsId: 'pcf-cre', organization: 'Northeast / Canada', target: 'https://api.run.pivotal.io'
					echo params.RELEASE_VERSION
					withCredentials([usernamePassword(credentialsId: 'pcf-cre', passwordVariable: 'SECREAT_PCF_PASSWORD', usernameVariable: 'SECREAT_PCF_USER')]) {
						//bat "cf login -a https://api.run.pivotal.io"
						def artifactory_server = Artifactory.server "Artifactory"
						bat 'cf login -a https://api.run.pivotal.io -u '+SECREAT_PCF_USER+' -p '+SECREAT_PCF_PASSWORD+' -o "Northeast / Canada" -s "bcbsma"'
						bat "cf env "+ARTIFACT_ID+" > temp.txt"
						//bat "${mvnHome}/bin/mvn clean package -P artifact-download -DgroupId="+GROUP_ID+" -DartifactId="+ARTIFACT_ID+" -Dversion="+ARTIFACT_VERSION+" "
						bat "cf push "+ARTIFACT_ID+" --no-manifest set-env APP_VERSION "+ARTIFACT_VERSION+" -p target/"+ARTIFACT_ID+"-"+appVersion+".jar --no-start --no-route"
						bat "cf map-route "+ARTIFACT_ID+" "+testUrl+""
						bat "cf restage "+ARTIFACT_ID+""
					}
					//pushToCloudFoundry cloudSpace: 'bcbsma', credentialsId: 'pcf-cre', manifestChoice: [appName: ARTIFACT_ID+"-test", appPath: 'target/'+ARTIFACT_ID+'-'+appVersion+'.jar', buildpack: '', command: '', domain: '', envVars: [[key: 'APP_VERSION', value: appVersion]], hostname: testUrl, instances: '1', memory: '758', noRoute: 'false', stack: '', timeout: '60', value: 'jenkinsConfig'], organization: 'Northeast / Canada', target: 'https://api.run.pivotal.io'	
				}
            }
            post {
				failure {
					//pushToCloudFoundry cloudSpace: 'bcbsma', credentialsId: 'pcf-cre', organization: 'Northeast / Canada', target: 'https://api.run.pivotal.io'
					script {
						//pushToCloudFoundry cloudSpace: 'bcbsma', credentialsId: 'pcf-cre', organization: 'Northeast / Canada', target: 'https://api.run.pivotal.io'
						def reader = new BufferedReader(new InputStreamReader(new FileInputStream("$WORKSPACE/temp.txt"),"UTF-8"))
						if(reader.find("APP_VERSION")) {
							
						}
						bat "cf"
					}
			    }
			}
        }
        stage ('Deploy Proxy') {
        agent any
	        when { expression { params.ENV_DEPLOY == 'deploy-proxy-dev' } }
	            steps {
	                echo 'Deploying proxy...'
					script {
						withCredentials([usernamePassword(credentialsId: 'apigee-sandesh', passwordVariable: 'SECREAT_APIGEE_PASSWORD', usernameVariable: 'SECREAT_APIGEE_USER')]) {
							withMaven(maven: 'maven') { 
								if(isUnix()) {
									sh "mvn clean install -DskipTests -Djacoco.skip=false -Djacoco.skip.report=false " 
								} else { 
									bat "mvn install -f src/main/resources/apigee/apigee-pom.xml -Pdev -Dusername=$SECREAT_APIGEE_USER -Dpassword=$SECREAT_APIGEE_PASSWORD -Dorg=bcbsma -Doptions=validate " 
								} 
						    
						    
						}
						/* withMaven(maven: 'maven') { 
							if(isUnix()) {
								sh "mvn clean install -DskipTests -Djacoco.skip=false -Djacoco.skip.report=false " 
							} else { 
								bat "mvn install -P $ENVIRONMENT -Dusername=$APIGEE_USERNAME -Dpassword=$APIGEE_PASSWORD -Dorg=bcbsma -Doptions=validate "  
							} 
						} */
					}
	            }
	        }
        }
        stage ('KVM Updated') {
        agent any
	        when { expression { params.ENV_DEPLOY == 'deploy-kvm-dev' } }
            steps {
                echo 'Updating KVM...'
				script {
					def secretText="U2FuZGVzaC5HYXdhbGlAcGVyZmljaWVudC5jb206QXBpZ2VlQDIwMTk="
					def filename="microgateway-router"
					def jsonSlurper = new JsonSlurper()
					def reader = new BufferedReader(new InputStreamReader(new FileInputStream("$WORKSPACE/apigee/microgateway-router.json"),"UTF-8"))
    				def data = jsonSlurper.parse(reader)
    				def entryName = data.name
    				jsonSlurper = null
    				data = null
    				reader = null
					//def URL="https://api.enterprise.apigee.com/v1/organizations/bcbsma/environments/$ENVIRONMENT/keyvaluemaps/microgateway-router/entries/$entryName"
					def URL="https://api.enterprise.apigee.com/v1/organizations/bcbsma/environments/dev/keyvaluemaps/microgateway-router/entries/${entryName}"
					def URL1 = "https://api.enterprise.apigee.com/v1/organizations/bcbsma/environments/dev/keyvaluemaps/microgateway-router/entries"
				    					
					bat 'curl -S -k --silent -X GET --header "Authorization: Basic '+secretText+'" '+URL+' --write-out "HTTPSTATUS=%%{http_code}" > result.txt'
					applyKvm(secretText, filename, URL, URL1)
				}
            }
        }
	}
}

def applyKvm(String secretText, String filename, String URL, String URL1) {
	def HTTP_RESPONSE = readFile 'result.txt'
	//def HTTP_BODY = (HTTP_RESPONSE =~ 'HTTPSTATUS=[0-9]{3}$')
	//println "HTTP_BODY = " + HTTP_BODY[0]
	//def HTTP_BODY = ~'s/HTTPSTATUS\:[0-9]{3}$//'
	//println HTTP_BODY.size()
	//def HTTP_RESPONSE_BODY = (HTTP_RESPONSE - HTTP_BODY[0])
	def HTTP_CODE = HTTP_RESPONSE.substring(HTTP_RESPONSE.lastIndexOf("=")+1)
	println "HTTP_CODE = " + HTTP_CODE
	println "filename = " + filename
	String fileContents = new File(''+WORKSPACE+'/apigee/microgateway-router.json').text
	//def routerContent = readFile 'apigee/microgateway-router.json'
	fileContents = fileContents.replaceAll("\\r\\n|\\r|\\n", " ");
	fileContents = fileContents.replace("\n", "").replace('\"', '\\"');
	//fileContents = '{"name": "edgemicro_sample_app_1","value": "https://mocktarget.apigee.net"}'
	println "fileContents = " + fileContents
	if (HTTP_CODE == "200") {
		//update an entry that already exist
		bat 'curl -S -k --silent -X POST --header "Content-Type: application/json" --header "Authorization: Basic '+secretText+'" --data "'+fileContents+'" '+URL+' --write-out "HTTPSTATUS1=%%{http_code}" > result1.txt'
		String UPDATE_RESPONSE = new File(''+WORKSPACE+'/result1.txt').text
		//def UPDATE_RESPONSE = readFile 'result.txt'
		println "UPDATE_RESPONSE = " + UPDATE_RESPONSE
	} else {
		//entry doesn’t exist create new entry
		println "1151"
		bat 'curl -S -k --silent -X POST --header "Content-Type: application/json" --header "Authorization: Basic '+secretText+'" --data "'+fileContents+'" '+URL1+' --write-out "HTTPSTATUS2=%%{http_code}" > result2.txt'
		String CREATE_RESPONSE = new File(''+WORKSPACE+'/result2.txt').text
		println "CREATE_RESPONSE = " + CREATE_RESPONSE
	}
}

def updateNextBuildNumber(currentBuildNumber, releaseVersion) {
	def curRelNum = '1'
	def prvRelNum = '0'
	def nextBuildNumber
	def buildIncNum = '1'
	def buildIncNumToInt
	def curBuildNumber = currentBuildNumber

	if(curBuildNumber != null && curBuildNumber.contains('-B')) {
		curRelNum = curBuildNumber.substring(0, curBuildNumber.indexOf('-B'))
		buildIncNum = curBuildNumber.substring(curBuildNumber.indexOf('-B') + 2, curBuildNumber.lastIndexOf("-"))
		buildIncNumToInt = buildIncNum.isInteger() ? buildIncNum.toInteger() + 1 : buildIncNum

		if(releaseVersion.equalsIgnoreCase(curRelNum)) {
			nextBuildNumber = curRelNum + '-B' + buildIncNumToInt + '-' + BUILD_NUMBER
		} else {
			nextBuildNumber = releaseVersion + '-B' + 1 + '-' + BUILD_NUMBER
		}
	} else {
		nextBuildNumber = releaseVersion + '-B' + 1 + '-' + BUILD_NUMBER
	}
	$BUILD_NUMBER =nextBuildNumber
	
	return nextBuildNumber
}