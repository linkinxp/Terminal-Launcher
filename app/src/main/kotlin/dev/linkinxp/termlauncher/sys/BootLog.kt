package dev.linkinxp.termlauncher.sys

val bootLog: List<String> = listOf(
    "[ ok ] mounting /dev/phone",
    "[ ok ] loading themes",
    "[ ok ] scanning packages",
    "[ ok ] syncing clock",
    "[ ok ] starting launcherd",
    "[ ok ] user@phone:~$ ready",
)

/** True when an unlock at [unlockAtMs] is recent enough ([windowMs]) for the boot log to still play. */
fun bootWindowOpen(nowMs: Long, unlockAtMs: Long, windowMs: Long = 3_000): Boolean =
    unlockAtMs > 0 && nowMs - unlockAtMs in 0..windowMs
