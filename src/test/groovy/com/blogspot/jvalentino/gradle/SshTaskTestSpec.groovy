package com.blogspot.jvalentino.gradle

import org.gradle.api.Project
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.testfixtures.ProjectBuilder

import com.jcraft.jsch.ChannelExec
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session

import spock.lang.Specification
import spock.lang.Subject

class SshTaskTestSpec extends Specification {

    @Subject
    SshTask task
    Project project
    
    def setup() {
        Project p = ProjectBuilder.builder().build()
        task = p.task('ssh', type:SshTask)
        task.instance = Mock(SshTask)
        project = Mock(ProjectInternal)
    }
    
    void "test perform"() {
        given:
        Map props = [
            'username':'foo', 
            'password':'bar', 
            'host':'blah.com'
        ]
        
        Session session = Mock(Session)
        
        when:
        task.perform()
        
        then:
        1 * task.instance.project >> project
        1 * project.properties >> props
        
        and:
        1 * task.instance.generateSession(
                props.username, props.password, props.host) >> 
            session
        1 * session.connect()
        
        and:
        1 * task.instance.executeCommand(
                'whoami', session) >> 'blahuser'
        1 * task.instance.executeCommand(
                'pwd', session) >> '/blah'
        1 * task.instance.executeCommand(
                'ls -la', session) >> 
                new File('src/test/resources/ls.txt').text
            
        and:
        1 * session.disconnect()
        
        and:
        task.systemUser == 'blahuser'
        task.userDir == '/blah'
        task.dirs.toString() == 
            '[., .., .CFUserTextEncoding, .bash_history, Desktop]'
    }
    
    void "test generateSession"() {
        given:
        String username = 'foo'
        String password = 'bar'
        String host = 'blah'
        
        GroovyMock(JSch, global:true)
        JSch jsch = Mock(JSch)
        
        Session session = Mock(Session)
        
        when:
        Session result = task.generateSession(
            username, password, host)
        
        then:
        1 * new JSch() >> jsch
        1 * jsch.getSession(
                username, host, 22) >> session
        1 * session.setPassword(password)
        1 * session.setConfig('StrictHostKeyChecking', 'no')
        
        and:
        result != null
    }
    
    void "test executeCommand"() {
        given:
        String command = 'ls'
        Session session = Mock(Session)
        ChannelExec channelExec = Mock(ChannelExec)
        InputStream is = Mock(InputStream)
        InputStream.class.metaClass.getText = {
            'blah'
        }
        
        when:
        String r = task.executeCommand(command, session)
        
        then:
        1 * session.openChannel('exec') >> channelExec
        1 * channelExec.inputStream >> is
        1 * channelExec.setCommand(command)
        1 * channelExec.connect()
        1 * channelExec.disconnect()
        
        and:
        r == 'blah'
    }
}
