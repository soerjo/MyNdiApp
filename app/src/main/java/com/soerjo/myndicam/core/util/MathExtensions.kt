package com.soerjo.myndicam.core.util

/**
 * Extension functions for math operations
 */

/**
 * Format aspect ratio as a simplified string (e.g., "16:9", "4:3")
 */
fun formatAspectRatio(width: Int, height: Int): String {
    val gcd = gcd(width, height)
    val aspectWidth = width / gcd
    val aspectHeight = height / gcd
    return "$aspectWidth:$aspectHeight"
}

/**
 * Calculate greatest common divisor using Euclidean algorithm
 */
fun gcd(a: Int, b: Int): Int {
    return if (b == 0) a else gcd(b, a % b)
}
