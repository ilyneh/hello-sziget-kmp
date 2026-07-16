package com.ilyne.helloszigetkmp.util.text

/**
 * Derives initials from a display name, e.g. "Zack Jones" -> "ZJ".
 *
 * Splits on single spaces and takes the uppercased first character of each non-blank segment,
 * so extra/leading/trailing spaces are safely ignored rather than throwing.
 */
fun String.initials(): String =
    split(" ")
        .mapNotNull { it.firstOrNull()?.uppercase() }
        .joinToString(separator = "")
