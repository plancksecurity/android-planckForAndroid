package com.fsck.k9.pEp.infrastructure.exceptions

class NotEnoughSpaceInDeviceException(
    val neededSpace: Long,
    val availableSpace: Long,
) : RuntimeException()
