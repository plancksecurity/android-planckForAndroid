package com.fsck.k9.pEp.infrastructure.exceptions

class NotEnoughSpaceInDeviceException(
    val neededSpace: Long,
    val availableSpace: Long,
) : RuntimeException(
    "ERROR: Not enough space available to export pEp data: " +
            "needed space is $neededSpace, available space is $availableSpace"
)
