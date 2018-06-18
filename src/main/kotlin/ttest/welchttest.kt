package ttest

// From https://rosettacode.org/wiki/Welch%27s_t-test#Kotlin

// version 1.1.4-3

typealias Func = (Double) -> Double

fun square(d: Double) = d * d

fun sampleVar(da: DoubleArray): Double {
    if (da.size < 2) throw IllegalArgumentException("Array must have at least 2 elements")
    val m = da.average()
    return da.map { square(it - m) }.sum() / (da.size - 1)
}

fun welch(da1: DoubleArray, da2: DoubleArray): Double {
    val temp = sampleVar(da1) / da1.size + sampleVar(da2) / da2.size
    return (da1.average() - da2.average()) / Math.sqrt(temp)
}

fun degreesFreedom(da1: DoubleArray, da2: DoubleArray): Double {
    val s1 = sampleVar(da1)
    val s2 = sampleVar(da2)
    val n1 = da1.size
    val n2 = da2.size
    val temp1 = square(s1 / n1 + s2 / n2)
    val temp2 = square(s1) / (n1 * n1 * (n1 - 1)) + square(s2) / (n2 * n2 * (n2 - 1))
    return temp1 / temp2
}

fun gamma(d: Double): Double {
    var dd = d
    val p = doubleArrayOf(
            0.99999999999980993,
            676.5203681218851,
            -1259.1392167224028,
            771.32342877765313,
            -176.61502916214059,
            12.507343278686905,
            -0.13857109526572012,
            9.9843695780195716e-6,
            1.5056327351493116e-7
    )
    val g = 7
    if (dd < 0.5) return Math.PI / (Math.sin(Math.PI * dd) * gamma(1.0 - dd))
    dd--
    var a = p[0]
    val t = dd + g + 0.5
    for (i in 1 until p.size) a += p[i] / (dd + i)
    return Math.sqrt(2.0 * Math.PI) * Math.pow(t, dd + 0.5) * Math.exp(-t) * a
}

fun lGamma(d: Double) = Math.log(gamma(d))

fun simpson(a: Double, b: Double, n: Int, f: Func): Double {
    val h = (b - a) / n
    var sum = 0.0
    for (i in 0 until n) {
        val x = a + i * h
        sum += (f(x) + 4.0 * f(x + h / 2.0) + f(x + h)) / 6.0
    }
    return sum * h
}

fun p2Tail(da1: DoubleArray, da2: DoubleArray): Double {
    val nu = degreesFreedom(da1, da2)
    val t = welch(da1, da2)
    val g = Math.exp(lGamma(nu / 2.0) + lGamma(0.5) - lGamma(nu / 2.0 + 0.5))
    val b = nu / (t * t + nu)
    val f: Func = { r ->  Math.pow(r, nu / 2.0 - 1.0) / Math.sqrt(1.0 - r) }
    return simpson(0.0, b, 10000, f) / g   // n = 10000 seems more than enough here
}