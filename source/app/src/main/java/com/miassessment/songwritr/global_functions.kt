package com.miassessment.songwritr

import android.annotation.SuppressLint
import android.content.Context
import android.database.Cursor
import android.database.Cursor.*
import android.media.MediaMetadataRetriever
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.view.View
import android.view.inputmethod.InputMethodManager
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

// This file contains functions that are used across activities, and is a way of keeping global functions organised in once location

const val REQUEST_CODE = 200 // used on AudioRecordActivity.kt

// gets data from a specified column when passed a single-row cursor
// used in Note.kt
fun getColumnString(cursor: Cursor?, column: String): String {
    cursor!!.moveToFirst()
    val index = cursor.getColumnIndex(column)
    return if (cursor.getType(index) == FIELD_TYPE_STRING) cursor.getString(index) else ""

    // the implementation of the if statement above is loosely based on information found here https://stackoverflow.com/questions/47626959/android-illegalstateexception-cursor-getstringidx-must-not-be-null#answer-47627011
}

fun getColumnInt(cursor: Cursor?, column: String): Int {
    cursor!!.moveToFirst()
    val index = cursor.getColumnIndex(column)

    // if field is a string, convert to integer and return
    return when (cursor.getType(index)) {
        FIELD_TYPE_INTEGER -> cursor.getInt(index)
        FIELD_TYPE_STRING -> cursor.getString(index).toInt()
        else -> -1
    }
}

@SuppressLint("Range")
fun getColumnBool(cursor: Cursor?, column: String): Boolean {
    cursor!!.moveToFirst()
    return if (cursor.getType(cursor.getColumnIndex(column)) == FIELD_TYPE_NULL) false else intToBool(getColumnInt(cursor, column))
}

fun getColumnDateTime(cursor: Cursor?, column: String): LocalDateTime {
    cursor!!.moveToFirst()
    return if (getColumnString(cursor, column) == "") LocalDateTime.parse("1998-02-15T22:00:00") else LocalDateTime.parse(getColumnString(cursor, column))
}

fun intToBool(int: Int): Boolean {
    return int == 1
}

//  to standardise the splitting/joining of the list of audio IDs attached to a note
fun joinAudioSetToString(set: MutableSet<Int>): String {
    return set.joinToString(",")
}

fun splitAudioStringToSet(string: String): MutableSet<Int> {
    val list = if (string.isNotEmpty()) string.split(",") else listOf()
    val mutableSet = mutableSetOf<Int>()
    for (item in list) mutableSet.add(item.toInt())

    return mutableSet
}

// returns ordinals when passed a day of the month
fun addOrdinals(day: Int): String {
    return "$day${getOrdinals(day)}"
}

fun getOrdinals(day: Int): String {
    return if (day % 10 == 1 && day % 100 != 11) "st"
           else if (day % 10 == 2 && day % 100 != 12) "nd"
           else if (day % 10 == 3 && day % 100 != 13) "rd"
           else "th"
}

fun patternWithOrdinals(pattern: String, date: LocalDate): DateTimeFormatter {
    // unlike above, this will replace the pattern string dddd with dd + correct ordinals
    // updated to also replace ddd with d + ordinals
    return DateTimeFormatter.ofPattern(
        pattern.replace("dddd", "dd'${getOrdinals(date.dayOfMonth)}'")
               .replace("ddd", "d'${getOrdinals(date.dayOfMonth)}'")
    )
}

fun patternWithOrdinals(pattern: String, dateTime: LocalDateTime): DateTimeFormatter {
    return patternWithOrdinals(pattern, dateTime.toLocalDate())
}

fun capitalize(string: String): String {
    return string.substring(0, 1).uppercase() + string.substring(1, string.length).lowercase()
}

// Source for the following two functions: https://www.youtube.com/watch?v=CrZ3kgWXKb0
fun convertPxToDp(px: Int, density: Float): Int { return ( px / density ).roundToInt() }
fun convertDpToPx(dp: Int, density: Float): Int { return ( dp * density ).roundToInt() }
// end

// The below function is from the following source: https://stackoverflow.com/a/58605532
fun checkInternetConnection(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val n = cm.activeNetwork
        if (n != null) {
            val nc = cm.getNetworkCapabilities(n)
            return nc!!.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) || nc.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        }
        return false
    } else {
        val netInfo = cm.activeNetworkInfo
        return netInfo != null && netInfo.isConnectedOrConnecting
    }
}

// This function adapts code from the following source: https://stackoverflow.com/questions/15394640/get-duration-of-audio-file
fun getAudioDuration(path: String, letters: Boolean = false): String {
    val mmr = MediaMetadataRetriever()
    mmr.setDataSource(path)
    val duration = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)!!.toInt()
    return formatAudioDuration(duration, letters)
}

fun formatAudioDuration(millis: Int, letters: Boolean = false): String {
    val h = millis / 3600000
    val m = millis % 3600000 / 60000
    val s = millis % 60000 / 1000

    if (letters && h == 0 && m == 0 && s == 0) return "0s"

    return if (letters) "${if (h > 0) "${h}h " else ""}${if (m > 0) "${m}m " else ""}${if (s > 0) "${s}s " else ""}".trim()
           else "${if (h > 0) "$h:" else ""}${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}"
}

// This function is from the following source: https://stackoverflow.com/a/47907489
fun View.hideKeyboard() {
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.hideSoftInputFromWindow(windowToken, 0)
}