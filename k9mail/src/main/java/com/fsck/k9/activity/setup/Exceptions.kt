package com.fsck.k9.activity.setup

class UnsuitableBrowserFound : RuntimeException()
class WrongEmailAddressException(val adminEmail: String, val userWrongEmail: String): Exception()