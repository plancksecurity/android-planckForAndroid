
    FILE1=src/androidTest/java/com/fsck/k9/ui/AccountSetupScreenshotTest.kt
    FILE2=src/androidTest/java/com/fsck/k9/ui/MessageListScreenshotTest.kt
    FILE3=src/androidTest/java/com/fsck/k9/ui/MessageViewScreenshotTest.kt
    FILE4=src/androidTest/java/com/fsck/k9/ui/MessageComposeScreenshotTest.kt
    FILE5=src/androidTest/java/com/fsck/k9/ui/SyncScreenshotTest.kt
    FILE6=src/androidTest/java/com/fsck/k9/ui/SettingsScreenshotTest.kt

    ################################################################################
    #                                Select GNU SED                                #
    ################################################################################

    OS="$(uname -s)"

    case "${OS}" in
        Linux*)     SED=sed;;
        Darwin*)    SED=gsed;;
        CYGWIN*)    echo "UNSUPORTED YET" && exit;;
        MINGW*)     echo "UNSUPORTED YET" && exit;;
        *)          echo "UNKNOWN:${OS}" && exit;;
    esac

    ################################################################################
    #              Remove @Ignore                                                  #
    ################################################################################


  if [ $1 == enable ]; then
      $SED -i '/@Ignore/d' $FILE1
      $SED -i '/@Ignore/d' $FILE2
      $SED -i '/@Ignore/d' $FILE3
      $SED -i '/@Ignore/d' $FILE4
      $SED -i '/@Ignore/d' $FILE5
      $SED -i '/@Ignore/d' $FILE6
  elif [ $1 == disable ]; then
      $SED -i 's/@Test/@Test\
    @Ignore/' $FILE1
      $SED -i 's/@Test/@Test\
    @Ignore/' $FILE2
      $SED -i 's/@Test/@Test\
    @Ignore/' $FILE3
      $SED -i 's/@Test/@Test\
    @Ignore/' $FILE4
      $SED -i 's/@Test/@Test\
    @Ignore/' $FILE5
      $SED -i 's/@Test/@Test\
    @Ignore/' $FILE6
  fi

