# planck

planck is an open-source email client for Android with encryption.

## How to start development
### Pre-requisistes
OS: MacOS or Linux.
The following dependencies are needed to build the project:
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
- asn1c
- yml2
### Clone the project
`git clone git@github.com:plancksecurity/android-planckForAndroid.git --recursive`

### Add custom .conf files as needed
You can add custom .conf files for core repos in `localConfFiles/<repo-name>` folder. (See as an example `localConfFiles/planckJNIWrapper/local.conf.sample`).

### Run the project
Click on "run" action on Android Studio or other IDE.

### Common issues
If you deleted some submodules and they are not coming back with `git submodule update`, you can either run `sh scripts/addSubmodules.sh` or just copy the line of the script for the submodule that has the conflict.


## Need Help?

If the app is not behaving like it should, you can access the user manual for the specific version from the app options menu and Settings/About screens.

## License

    Licensed under the GNU General Public License, Version 3.0.

