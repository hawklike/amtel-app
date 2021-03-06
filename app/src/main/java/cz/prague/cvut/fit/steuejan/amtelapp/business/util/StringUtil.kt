package cz.prague.cvut.fit.steuejan.amtelapp.business.util

import java.util.*

object StringUtil
{
    fun getRandomString(length: Int) : String
    {
        val allowedChars = "abcdefghiklmnopqrstuvwxyz1234567890"
        return (1..length)
            .map { allowedChars.random() }
            .joinToString("")
    }

    //creates an acronym of the first three letters in the text
    fun createLabel(text: String): CharSequence
    {
        return text.fold(StringBuilder()) { acc, c ->
            if(acc.length > 2) return@fold acc
            acc.append(c)
        }
    }

    fun prepareCzechOrdering(name: String, surname: String): Pair<String, String>
    {
        val convertedName = changeToEnglishTranscription(name)
        val convertedSurname = changeToEnglishTranscription(surname)

        return Pair(convertedName, convertedSurname)
    }

    fun prepareCzechOrdering(text: String): String
            = changeToEnglishTranscription(text)

    private fun changeToEnglishTranscription(text: String): String
    {
        return text.toLowerCase(Locale.getDefault())
            .replace("č", "czz")
            .replace("ch", "hzz")
            .replace("ď", "dzz")
            .replace("ř", "rzz")
            .replace("š", "szz")
            .replace("ť", "tzz")
            .replace("ž", "zzz")
    }
}

fun String.firstLetterUpperCase(): String
{
    val output = this.toLowerCase()
    return output.replaceRange(0, 1,
        this[0]
            .toUpperCase()
            .toString())
}

fun String.shrinkWhitespaces(): String =
    this.replace("\\s+".toRegex(), " ")

fun String.removeWhitespaces(): String =
    this.replace("\\s".toRegex(), "")