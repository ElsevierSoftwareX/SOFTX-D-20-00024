# Let's install PeptideMind on your system!

Ok, hold onto your socks because installing PeptideMind will take a little bit of patience (~30 mins). Follow these steps and you should be fine:

### What you’ll be downling (and why):
	Git
		To grab a copy of the PeptideMind source code
	Java JDK 8
		Most of PeptideMind runs on Java – so we’ll need an appropriate java platform
    Gradle 6
	    Gradle helps create an environment on your computer that can run PeptideMind 
    Python 3.6
	    The brains of PeptideMind – handles all ML code
	Kotlin 1.3.21
		The beauty of PeptideMind – handles GUI code and some data-wrangling

### Instructions
1.	First, we’re going to install Java.

    a. [Click this link](https://github.com/AdoptOpenJDK/openjdk8-binaries/releases/download/jdk8u265-b01/OpenJDK8U-jdk_x64_windows_hotspot_8u265b01.msi) and follow the installation instructions.

    b. Restart your system

2.	Next, we need python 3.6.

    a. [Click this link](https://www.python.org/ftp/python/3.6.7/python-3.6.7-amd64.exe) and follow the instructions.

    b. Make sure that you select the Add to Path option on the first page.

    c. Restart your system

3.	Download GIT.

    a. [Click this link](https://github.com/git-for-windows/git/releases/download/v2.28.0.windows.1/Git-2.28.0-64-bit.exe) and follow the instructions.

    b. Git has many bells/whistles and options to select. Choose what is appropriate for your system; most, if not all, configurations will work.

    c. Restart your system

4.	Gradle.

    a. [This one is more involved and you'll need to follow a tutorial](https://docs.gradle.org/current/userguide/installation.html)
    
5.	Kotlin. Easy one:

    a. [Click this link](https://github.com/JetBrains/kotlin/releases/download/v1.3.21/kotlin-native-windows-1.3.21.zip) and follow the instructions.

    b. Restart your system

6.	Check everything is working!

    a.	Open up your command prompt and type in the following commands separately followed by enter:

    `python`

    `javac -version`

    `java -version`

    `gradle`

    `kotlinc`

    `git`

    b.	If the packages were installed correctly, you should see each program execute. If anything fails to load, you'll have to troubleshoot why it's not loading correctly. In most cases, the system is probably missing what's called an environment variable (when you type in `javac`, for instance, the computer needs to be told what that means. Installing development tools will often handle this for you but sometimes (Python, I'm looking at you) it wont.)

7.	Time to get PeptideMind’s code!

    a.	Make a folder on your computer where you want to place the code. I recommend `C:\Users\Your_UserName\code\`

    b.	Open up command prompt and type: `cd C:\Users\Your_UserName\code\`

    c.	Next: `git clone https://bitbucket.org/peptidewitch/peptidemind.git`

    d.	This will create a new folder called peptidemind and download all of PeptideMind into the code folder. All of the source code for the project should now be in there.

8.	Time to set up the Python environment for PeptideMind.

    a.	With command prompt, type the following command: `pip install poetry`

    b.	Navigate into your PeptideMind folder: `cd C:\Users\Your_UserName\code\peptidemind`

    c.	Next, type in `poetry install`

    d.  The system will spend the next 5 minutes installing the python environment for you. It's a little slow, sorry.

9.	Time to set up the Gradle environment for PeptideMind:

    a.	With command prompt, type the following command from the peptidemind folder: `gradle build`

10.	Last step!

    a.	To launch PeptideMind, run the following command from the peptidemind folder: `gradle run.`


It’s thaaaaaat easy. I’m sorry for wasting an hour of your life. Whenever you want to work with peptide mind, you can use that `gradle run` command to open it up. Alternatively, there's a standalone file in the folder `peptidemind/out/artefacts/kerberus_jar.jar`. It may not work. Spicy tip for the future - don't ever use Java for a PhD project. Don't build a GUI either. Put it all online like a sensible person.