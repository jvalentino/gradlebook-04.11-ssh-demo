## 4.11 Using SSH

Secure Shell (SSH) is a cryptographic network protocol for operating network services securely over an unsecured network. The best-known example application is for remote login to computer systems by users. SSH provides a secure channel over an unsecured network in a client-server architecture, connecting an SSH client application with an SSH server. Common applications include remote command-line login and remote command execution, but any network service can be secured with SSH. 

\-    https://en.wikipedia.org/wiki/Secure_Shell

SSH is a means by which commands can be securely executed on a remote computer. One will find that this is the underlying basis by which most deployment frameworks function. A common usage I have found with Gradle is for automating both simple and complex remote operations, to fill gaps either due to a non-existent third-party solution, or the inability to use one. The most common scenarios I come across are when dealing with remote configuration and deployment, because the deployment environment is either highly custom, or a third-party tool is not allowed.

​      The purpose of this plugin is to demonstrate the basics for SSH in Gradle, by making a plugin that remotes into a Linux style machine, does a directory listing, parses the output for directory names, and then writes those directory names to the command-line. This additionally shows how to use the advanced mocking capabilities of Spock, to handle unit testing the involved tasks and methods.

#### build.gradle

```groovy
dependencies {
    compile gradleApi()
    compile 'org.codehaus.groovy:groovy-all:2.4.12'
    compile 'com.jcraft:jsch:0.1.54'
    
    
    testCompile 'org.spockframework:spock-core:1.1-groovy-2.4'
    testCompile 'cglib:cglib:3.2.6'
    testCompile 'org.objenesis:objenesis:2.6'
}

```

The library being used for SSH is JCraft JSch, which is s pure Java implementation of SSH2.

#### src/main/groovy/com/blogspot/jvalenitno/gradle/SshTask.groovy

```groovy
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

```

**Line 24: Properties**

The strategy of using a self-reference that can be later mocked to obtain the project and its properties is used. The properties are needed because they are expected to contain the username, password, and host to be used in the SSH operations.

 

**Lines 26-27: SSH Session**

The details of establishing an SSH session using JSch were moved into another method. This was done to make the task operation clearer, and to make the testing of this method more straightforward. Otherwise, just testing the general flow will involve mocking complexity.

 

**Line 28: SSH session connection**

The SSH session must be connected to be able to perform any operations. Note that if there were a connection error or a problem with the credentials, and exception would be thrown here.

 

**Lines 30-35: The remote commands**

The remote commands are executed, and their output storied. The complexities of the remote command execution have also been moved into their own method, to make the main flow more readable and for better testability.

 

**Line 37: Ending the SSH session**

Once all the commands have been executed, terminates the session.

 

**Line 42: Directory output parsing**

The one piece of logic required is how to handle the directory output listing. This is delegated to a method to derive the directory names.

```groovy
    Session generateSession(String username, String password, 
        String host) {
        JSch jsch = new JSch()
        Session session = jsch.getSession(
                username, host, 22)
        session.password = password
        session.setConfig('StrictHostKeyChecking', 'no')
        session
    }

```

**Lines 46-54: Generating the SSH session**

The SSH session generation assumes that username and passwords will be used as credentials, and the port will be 22. A more secure connection option is to use a key file and username instead, and the port in use can also easily be changed. The purpose of the **StrictHostKeyChecking** setting to “no” is to allow the connection to be established even if the remote machine is not part of an explicit list of expected remote hosts.

```groovy
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

```

**Lines 56-71: Command execution**

Remote commands first require a channel of type “exec” to be opened and connected on the Session. An InputStream is then used to capture any text output created by the execution of that remote command. After the command is executed, the channel must be disconnected before another command will be able to run.

```groovy
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

```

**Lines 73-83: Directory parsing**

Takes the text output from “ls -la”, where the first line contains the count of matching files, and returns the directory names in a list.

 

#### plugin-tests/local/build.gradle

```groovy
buildscript {
  repositories {
	jcenter()
  }
  dependencies {
    classpath 'com.blogspot.jvalentino.gradle:ssh-demo:1.0.0'
  }
}

apply plugin: 'ssh-demo'

```

 Plugin application or testing purposes is the same as within any other plugin, where we rely on settings.gradle to associate the test project with the plugin project. Decalre the library as a classpath depednency, and apply the plugin by name.

#### plugin-tests/local/gradle.properties

```properties
username=testuser
password=OsOeKK7]x!O7q21#n;jUeQ8S&BVoEYv
host=localhost

```

The expected properties for username, password, and host have been placed in this file.

#### Manual Testing

```bash
plugin-tests/local$ gradlew ssh

> Task :ssh 
$ whoami
> testuser
$ pwd
> /Users/testuser
$ ls -la
> total 8
drwxr-xr-x+ 12 testuser  staff  408 Apr 21 11:07 .
drwxr-xr-x   7 root      admin  238 Apr 21 11:04 ..
-rw-------   1 testuser  staff    3 Apr 21 11:04 .CFUserTextEncoding
-rw-------   1 testuser  staff    9 Apr 21 11:07 .bash_history
drwx------+  3 testuser  staff  102 Apr 21 11:04 Desktop
drwx------+  3 testuser  staff  102 Apr 21 11:04 Documents
drwx------+  3 testuser  staff  102 Apr 21 11:04 Downloads
drwx------@ 26 testuser  staff  884 Apr 21 11:04 Library
drwx------+  3 testuser  staff  102 Apr 21 11:04 Movies
drwx------+  3 testuser  staff  102 Apr 21 11:04 Music
drwx------+  3 testuser  staff  102 Apr 21 11:04 Pictures
drwxr-xr-x+  5 testuser  staff  170 Apr 21 11:04 Public

System User: testuser
User Directory: /Users/testuser
Directories: [., .., .CFUserTextEncoding, .bash_history, Desktop, Documents, Downloads, Library, Movies, Music, Pictures, Public]


BUILD SUCCESSFUL

```

The execution of the “ssh” task from the custom plugins shows the results of the three commands.

#### src/test/groovy/com/blogspot/jvalenitno/gradle/SshTaskTestSpec.groovy

```groovy
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

```

**Lines 17-25: Standup setup**

The subject of the test and the **project** are kept as member variables, while the instance strategy for mocking self-reference is used. Additionally, the project is mocked to be used to later handle properties.

```groovy
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

```

**Lines 29-33: Properties**

A Map of the username, password, and host name-value pairs is created, to later be returned when the mocked project asked for the properties. It is best to use non-existent hosts and users, to prevent any possible accidental connection to a remote host. This is a unit test, not an integration or functional test.

 

**Line 35: Session mocking**

The Session instance is created as a mock, to be returned in the later call to generate the session.

 

**Line 38: Task execution**

Executes the task method under test.

 

**Lines 41-42: Properties mocking**

Uses the self-reference to the task to return the mocked project instance, which is then used to return the properties containing host, username, and password.

 

**Lines 45-47: Returning the mocked session**

The task self-reference is used to expect the call to the method used to generate the Session, when then returns the earlier mocked Session.

 

**Line 48: Session connection**

Asserts the expectation that the Session **connect** method is called.

 

**Lines 51-52: whoami**

The task self-reference I used to expect the call to the method used to execute the “whoami” command and returns the result.

 

**Lines 53-54: pwd**

The task self-reference I used to expect the call to the method used to execute the “pwd” command and returns the result.

 

**Lines 55-57: ls -la**

The task self-reference I used to expect the call to the method used to execute the “la -la” command. However, as the result is to be parsed it must be in a specific format. For this reason, the output of an actual “ls- la” command was put into a text file to be returned as the result, so that is can be successfully parsed.

 

**Line 60: Session disconnect**

The task self-reference is used to expect the call to disconnect the session.

 

**Lines 62-66: Assertions**

Asserts the three outputs of the task, being the user, the current directory, and the directory listing.

```groovy
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

```

**Lines 71-73: Inputs**

The username, password, and host are the inputs to the method under test.

 

**Lines 75-76: JSch**

The method under test relies on a new instance of JSch class, and since we don’t want to actually establish an SSH session, it must be mocked. GroovyMock is used to at a system level replace all occurrences of the JSch class with a mock, and additionally to create a new instance of that class as a mock.

 

**Line 78: Session mock**

The result of calling **jsch.getSession** is a session object, so this mock has been setup to later be returned from that call.

 

**Lines 81-82: Method execution**

Executes the method under test.

 

**Line 85: new JSch**

When a new instance of JSch is created, return the previously mocked instance instead.

 

**Lines 86-87: Getting the session**

Expect that when a single call is made to **jsch.getSession**, to return the previously mocked instance of Session.

 

**Lines 88-89: Session settings**

Expects calls to set both the password and the strict host setting on the session instance.

```groovy
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

```

**Line 97: Method input**

The input to the method under test is a command to be executed on the remote system.

 

**Lines 98-100: Mocks**

Command execution involves Session, ChannelExec, and InputStream instances, so mocks are setup for each.

 

**Lines 101-103: InputStream metaprogramming**

The method for getting text from the input stream involves a Groovy enhancement to the InputStream, which cannot be easily mocked. For this reason, the **getText** method definition is changed to return the value of “blah” for testing purposes.

 

**Lines 109-113: Session and Channel interactions**

The Session and Channel interactions are handled to assert that the channel is made, connected, has the command executed, and then is disconnected.

 

**Line 116: The assertion**

Asserts the result of the method, which is the text content of the mocked InputStream’s conversion to text.



