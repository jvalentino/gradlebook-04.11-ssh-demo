package com.blogspot.jvalentino.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

import com.jcraft.jsch.ChannelExec
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session

/**
 * <p>A Task.</p>
 * @author jvalentino2
 */
@SuppressWarnings(['Println'])
class SshTask extends DefaultTask {

    SshTask instance = this
    String systemUser
    String userDir
    List<String> dirs

    @TaskAction
    void perform() {
        Map props = instance.project.properties

        Session session = instance.generateSession(
                props.username, props.password, props.host)
        session.connect()

        systemUser = instance.executeCommand(
                'whoami', session)
        userDir = instance.executeCommand(
                'pwd', session)
        String dirText = instance.executeCommand(
                'ls -la', session)

        session.disconnect()

        println ''
        println "System User: ${systemUser}"
        println "User Directory: ${userDir}"
        dirs = parseDirectoyNames(dirText)
        println "Directories: ${dirs}"
    }

    Session generateSession(String username, String password, 
        String host) {
        JSch jsch = new JSch()
        Session session = jsch.getSession(
                username, host, 22)
        session.password = password
        session.setConfig('StrictHostKeyChecking', 'no')
        session
    }

    String executeCommand(String command, Session session) {
        ChannelExec channelExec = session.openChannel('exec')

        InputStream is = channelExec.inputStream

        println "\$ ${command}"
        channelExec.command = command
        channelExec.connect()

        String text = is.text.trim()
        println "> ${text}"

        channelExec.disconnect()

        text
    }

    List<String> parseDirectoyNames(String text) {
        List<String> results = []
        int index = 0
        text.eachLine { String line ->
            if (index != 0) {
                results.add(line.split(' ').last())
            }
            index++
        }
        results
    }
}
