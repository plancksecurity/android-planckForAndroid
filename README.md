# planck

planck is an open-source email client for Android with encryption.

## How to start development
### Pre-requisistes
OS: MacOS or Linux.
The following dependencies are needed to build the project:
- XCode command line tools
- openjdk@11
- wget
- md5sha1sum
- gnu-sed
- autoconf
- automake
- libtool
- python
- pkg-config
- rust
- lxml
- rosetta
- asn1c **(expected to be installed at system level**, otherwise see custom .conf files can be used as detailed below)
- yml2 (**expected to be in \$HOME/yml2 by default**, otherwise see custom .conf files can be used as detailed below)

**If you are building on a second machine, you may need to initially copy the files at .ssh folder to the same folder on your new machine so you can access GitHub repos via ssh.**

## Setup process on an M1 Mac
Previous step: Install the XCode command line tools: Run `xcode-select --install` on a terminal (you can use `xcode-select -p` to check if they are already installed), then just follow the wizard instructions and wait until completion.

### MacOS M1: Run the setup script on a clean machine
On MacOS it is recommended to use the script scripts/setup/setupM1ForP4AMacPorts.sh to fully setup the machine from factory for development of this project.
The script has been tested on a M1 Mac.
At the end of the script you will be directed to the Downloads page of Android Studio. Download and install it. Then Android Studio will automatically open, just follow the wizard instructions on the first run.

In the case that the machine is not from factory and there are already some dependencies etc installed, still parts of the script can be copied/pasted and executed.

### Clone the project
`git clone git@github.com:plancksecurity/android-planckForAndroid.git --recursive`

### Open the project in Android Studio
Android studio will download all required Android dependencies once the project is open, wait for a while until that process is completed.

### Add custom .conf files as needed
You can add custom .conf files for core repos in `localConfFiles/<repo-name>` folder. (See as an example `localConfFiles/planckJNIWrapper/local.conf.sample`).
No files are needed by default.

### Run the project
Click on "run" action on Android Studio or other IDE.

### Common issues
If you deleted some submodules and they are not coming back with `git submodule update`, you can either run `sh scripts/addSubmodules.sh` or just copy the line of the script for the submodule that has the conflict.


## Need Help?

If the app is not behaving like it should, you can access the user manual for the specific version from the app options menu and Settings/About screens.

## License

    Licensed under the GNU General Public License, Version 3.0.

