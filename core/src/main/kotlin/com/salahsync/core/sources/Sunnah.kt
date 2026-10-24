package com.salahsync.core.sources

import com.salahsync.core.adhkar.HadithRef

/** Builds [HadithRef]s that link to sunnah.com, whose numbering every reference here follows. */
object Sunnah {
    const val BASE = "https://sunnah.com/"

    fun bukhari(n: String, narrator: String) =
        HadithRef("Sahih al-Bukhari", n, narrator, "Sahih (in Sahih al-Bukhari)", BASE + "bukhari:$n")

    fun muslim(n: String, narrator: String) =
        HadithRef("Sahih Muslim", n, narrator, "Sahih (in Sahih Muslim)", BASE + "muslim:$n")

    fun abuDawud(n: String, narrator: String, grade: String) =
        HadithRef("Sunan Abi Dawud", n, narrator, grade, BASE + "abudawud:$n")

    fun tirmidhi(n: String, narrator: String, grade: String) =
        HadithRef("Jami' at-Tirmidhi", n, narrator, grade, BASE + "tirmidhi:$n")

    fun ibnMajah(n: String, narrator: String, grade: String) =
        HadithRef("Sunan Ibn Majah", n, narrator, grade, BASE + "ibnmajah:$n")
}
