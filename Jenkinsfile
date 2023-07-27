pipeline {
  agent any
  options {
    buildDiscarder(logRotator(numToKeepStr: '5'))
  }
  environment {
    HYUNDAITEL_CREDENTIALS = credentials('hyundaitelecom')
    version_value = ''
    version = ''
    imageName = ''
  }
  stages {
    stage('get version from gradle') {
      steps {
        script {
          version_value = sh(returnStdout: true, script: "cat build.gradle.kts | grep -o 'version = [^,]*'").trim()
          sh "echo Project in version value: $version_value"
          version = version_value.split(/=/)[1].trim().split(/"/)[1].trim()
          sh "echo final version: $version"
          imageName = repository + ":" + version
        }
      }
    }
    stage('Gradle Build') {
      steps {
        sh './gradlew clean build'
      }
    }
    stage('Docker Build') {
      steps {
        script {
            sh 'echo imageName: ' + imageName
            dockerImage = docker.build imageName
        }
      }
    }
    stage('scp to remote') {
      steps {
        script {
            imageFile = imageName + '.tar'
            sh 'echo save image >> ' + imageFile
            sh 'docker save $imageName > $imageFile'
            sh 'echo scp to remote ' + imageFile
            sh 'ls -l'
        }
      }
    }
  }
}