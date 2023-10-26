package com.miassessment.songwritr

import android.content.Intent
import android.content.res.Resources
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.squareup.picasso.Picasso
import okhttp3.*
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

class PromptsActivity : AppCompatActivity() {
    private var numWords = 5
    private var maxWords = 15
    private var wordRarity = 22
    private lateinit var imagePromptView: ImageView
    private lateinit var imagePromptAttr: TextView
    private lateinit var wordsTextV: TextView
    private lateinit var loadingText: TextView
    private lateinit var quoteTextV: TextView
    private lateinit var quoteAuthorV: TextView
    private lateinit var useBtn: Button
    private var density = 0f

    object CurrentPrompt {
        var type = ""
        var data = ""
    }

    object ImageSize {
        var width = 0
        var height = 0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_prompts)

        density = Resources.getSystem().displayMetrics.density

        imagePromptView = findViewById(R.id.imagePromptView)
        imagePromptAttr = findViewById(R.id.imagePromptAttr)
        wordsTextV = findViewById(R.id.wordsView)
        quoteTextV = findViewById(R.id.quoteView)
        quoteAuthorV = findViewById(R.id.quoteAuthorView)
        hideAllPromptViews()

        wordsTextV.movementMethod = ScrollingMovementMethod()

        loadingText = findViewById(R.id.loadingText)
        loadingText.visibility = View.GONE

        useBtn = findViewById(R.id.usePromptBtn)
        useBtn.visibility = View.INVISIBLE

        setWordBtnText()

        // TOOLBAR
        findViewById<ImageButton>(R.id.toolbarHome).setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }

    private fun hideAllPromptViews() {
        imagePromptView.visibility = View.GONE
        imagePromptAttr.visibility = View.GONE
        quoteTextV.visibility = View.GONE
        quoteAuthorV.visibility = View.GONE
        wordsTextV.visibility = View.GONE
    }

    fun promptBtnPressed(view: View) {
        if (!checkInternetConnection(this)) {
            Snackbar.make(this, view, getString(R.string.connect_to_internet), Snackbar.LENGTH_LONG).show()
            return
        }

        useBtn.visibility = View.INVISIBLE
        loadingText.visibility = View.VISIBLE
        loadingText.text = getString(R.string.prompt_loading_text)

        hideAllPromptViews()

        when (view.id) {
            R.id.imagePromptBtn -> {
                CurrentPrompt.type = "Image"
                getImage()
            }

            R.id.quotePromptBtn -> {
                CurrentPrompt.type = "Quote"
                getQuote()
            }

            R.id.wordPromptBtn -> {
                CurrentPrompt.type = "Words"
                getWords()
            }
        }
    }

    fun wordNumBtnPressed(view: View) {
        if (view.id == R.id.increaseWordsBtn && numWords < maxWords) {
            numWords++
        } else if (view.id == R.id.decreaseWordsBtn && numWords > 1) {
            numWords--
        }

        setWordBtnText()
    }

    private fun setWordBtnText() {
        val textV = findViewById<TextView>(R.id.wordPromptBtn)
        val btnText = "Get $numWords word${if (numWords > 1) "s" else ""}"
        textV.text = btnText
    }

    // clicking this button will take user to the songbook and create a new note with prompt attached
    fun useBtnPressed(view: View) {
        val intent = Intent(this, EditNoteActivity::class.java)
        intent.putExtra(getString(R.string.intent_has_prompt_key), true)
        intent.putExtra(getString(R.string.intent_prompt_type_key), CurrentPrompt.type)
        intent.putExtra(getString(R.string.intent_prompt_data_key), CurrentPrompt.data)

        startActivity(intent)
    }


    // APIs

    // adapted from Mobile Interaction Practical 4
    private fun getDataFromServer(url: String) {
        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()
        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                response.use{
                    if (!response.isSuccessful) throw IOException ("Unexpected code $response")
                    when (CurrentPrompt.type) {
                        "Image" -> readJSONImage(response.body!!.string())
                        "Words" -> readJSONWords(response.body!!.string())
                        "Quote" -> readJSONQuote(response.body!!.string())
                    }
                }
            }
        })
    }

    // QUOTES
    private fun getQuote() {
        val url = "https://zenquotes.io/api/random/"
        try {
            getDataFromServer(url)
        }
        catch (e: IOException) {
            loadingText.text = "Error: getQuote()"
            return
        }
    }

    private fun readJSONQuote(rawJson: String) {
        runOnUiThread(java.lang.Runnable {
            try {
                val json = JSONArray(rawJson)
                val quote = json.getJSONObject(0).getString("q")
                val author = json.getJSONObject(0).getString("a")

                CurrentPrompt.data = "q:$quote,a:$author"

                quoteTextV.text = "\"${quote.trim()}\""
                quoteAuthorV.text = "- $author"

                quoteTextV.visibility = View.VISIBLE
                quoteAuthorV.visibility = View.VISIBLE

                loadingText.visibility = View.GONE
                useBtn.visibility = View.VISIBLE
            }
            catch (e: JSONException) {
                loadingText.text = "Error: readJSONWords()"
            }
        })
    }

    // IMAGES
    private fun getImage() {
        val url = "https://api.unsplash.com/photos/random/?client_id=XwwSX4K9RyDtqa8DUGk5V-OaHbxpEEZiDJqhgri5xPU"

        try {
            getDataFromServer(url)
        }
        catch (e: IOException) {
            loadingText.text = "Error: getImage()"
            return
        }
    }

    private fun readJSONImage(rawJson: String) {
        runOnUiThread(java.lang.Runnable {
            try {
                val json = JSONObject(rawJson)
                val imgUrl = json.getJSONObject("urls").getString("small")
                val imgAttr = json.getJSONObject("user").getString("name")

                imagePromptView.visibility = View.VISIBLE
                fitImage(json.getInt("width"), json.getInt("height"))
                imagePromptView.layoutParams.width = convertDpToPx(ImageSize.width, density)
                imagePromptView.layoutParams.height = convertDpToPx(ImageSize.height, density)
                imagePromptView.requestLayout()


                Picasso.get()
                       .load(imgUrl)
                       .fit()
                       .centerInside()
                       .into(imagePromptView)

                CurrentPrompt.data = "url:$imgUrl,attr:$imgAttr"

                imagePromptAttr.visibility = View.VISIBLE
                imagePromptAttr.text = "\u00a9 $imgAttr"

                loadingText.visibility = View.GONE
                useBtn.visibility = View.VISIBLE
            }
            catch (e: JSONException) {
                loadingText.text = "Error: readJSONImage()"
            }
        })
    }

    private fun fitImage(width: Int, height: Int) {

        val maxWidth = 400
        val maxHeight = 300
        ImageSize.width = maxWidth
        ImageSize.height = maxHeight

        if (width > height) { ImageSize.height = height * maxWidth / width }
        else { ImageSize.width = width * maxHeight / height }
    }

    // WORDS
    private fun getWords() {
        val url = "https://api.wordnik.com/v4/words.json/randomWords?hasDictionaryDef=true&maxCorpusCount=-1&minDictionaryCount=$wordRarity&maxDictionaryCount=-1&minLength=5&maxLength=-1&limit=$numWords&api_key=m2nzvqo4e13awj00tkxe41kp6duva5nlx9unn6te7yndfdjya"

        try {
            getDataFromServer(url)
        }
        catch (e: IOException) {
            loadingText.text = "Error: getWords()"
            return
        }
    }

    private fun readJSONWords(rawJson: String) {
        runOnUiThread(java.lang.Runnable {
            try {
                val json = JSONArray(rawJson)
                val words = mutableListOf<String>()

                for (i in 0 until json.length()) words.add(json.getJSONObject(i).getString("word"))

                val wordsText = words.joinToString("\n")
                CurrentPrompt.data = words.joinToString(",")

                wordsTextV.scrollTo(0, 0)
                wordsTextV.text = wordsText.trim()
                wordsTextV.visibility = View.VISIBLE
                loadingText.visibility = View.GONE
                useBtn.visibility = View.VISIBLE
            }
            catch (e: JSONException) {
                loadingText.text = "Error: readJSONWords()"
            }
        })
    }
}