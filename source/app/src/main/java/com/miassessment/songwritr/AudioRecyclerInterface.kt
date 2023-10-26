package com.miassessment.songwritr

import android.view.View

// The use of an interface for Recycler View click listeners adapts methods and code from the following two tutorials:
// https://www.youtube.com/watch?v=7GPUpvcU1FE&t=332s
// https://aayushpuranik.medium.com/recycler-view-using-kotlin-with-click-listener-46e7884eaf59

interface AudioRecyclerInterface {
    fun onItemClick(view: View, audio: Audio)
    fun attachAudio(view: View, audioID: Int)
}