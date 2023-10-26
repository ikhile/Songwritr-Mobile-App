package com.miassessment.songwritr

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import okhttp3.*
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException


class DictionaryActivity : AppCompatActivity(), DictInterface {
    private lateinit var wordField: EditText
    private lateinit var resultsTitle: TextView
    private lateinit var rhymeBtn: Button
    private lateinit var defBtn: Button
    private lateinit var synBtn: Button
    private var defaultBtnColour = -1
    private var activeBtnColour = -1
    private var currentWord = ""
    private var currentBtn = ""

    // RECYCLER VIEW
    private lateinit var recyclerView: RecyclerView
    private lateinit var dictAdapter: DictionaryAdapter
    private lateinit var layoutManager: GridLayoutManager
    private var recyclerItemList = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dictionary)

        wordField = findViewById(R.id.wordField)
        resultsTitle = findViewById(R.id.resultsTitle)
        rhymeBtn = findViewById(R.id.rhymeBtn)
        defBtn = findViewById(R.id.defBtn)
        synBtn = findViewById(R.id.synBtn)

        defaultBtnColour = ContextCompat.getColor(this, R.color.dark_sky_blue)
        activeBtnColour = ContextCompat.getColor(this, R.color.vivid_burgundy)
        resetBtnColours()

        // if a word is passed via the intent, enter this into the text input
        if (!intent.getStringExtra(getString(R.string.intent_word_key)).isNullOrEmpty()) {
            wordField.setText(intent.getStringExtra(getString(R.string.intent_word_key)).toString().trim())
        }

        recyclerView = findViewById(R.id.dataRecycler)
        layoutManager = GridLayoutManager(applicationContext, 1)
        dictAdapter = DictionaryAdapter(recyclerItemList, this)
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = dictAdapter

        // TOOLBAR
        findViewById<ImageButton>(R.id.toolbarHome).setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onSearchPressed(input: CharSequence) {
        resetBtnColours()
        recyclerItemList.clear()
        dictAdapter.notifyDataSetChanged()
        resultsTitle.text = ""
        wordField.setText(input)
        wordField.setSelection(input.length) // move cursor to end of field
    }


    fun dictBtnPressed(view: View) {
        if (!checkInternetConnection(this)) {
            Snackbar.make(this, view, getString(R.string.connect_to_internet), Snackbar.LENGTH_LONG).show()
            return
        }

        val b = view as Button

        if (wordField.text.toString() != currentWord || view.text != currentBtn) {
            resetBtnColours()
            view.hideKeyboard()

            if (wordField.text.isEmpty()) Snackbar.make(wordField, "Please enter a search term.", Snackbar.LENGTH_LONG).show()

            else {
                currentWord = wordField.text.toString().trim()
                currentBtn = view.text.toString()
                b.setBackgroundColor(activeBtnColour)

                resultsTitle.text = "Loading ${currentBtn.lowercase()} for ${currentWord.lowercase()}..."

                // clear all items in recycler view and update
                recyclerItemList.clear()
                dictAdapter.notifyDataSetChanged()

                // reset recyclerView to 1 column layout - will set 2 for synonyms and rhymes
                layoutManager.spanCount = 1

                getDict(view)
            }
        }
    }

    private fun getDict(view: View) {
        var url = ""
        val inputText = wordField.text.trim()

        when (view.id) {
            R.id.defBtn -> url = "https://api.dictionaryapi.dev/api/v2/entries/en/$inputText"
            R.id.synBtn -> url = "https://www.dictionaryapi.com/api/v3/references/thesaurus/json/$inputText?key=33b17ea7-ad6e-4f93-a674-5ca0e7083702"
            R.id.rhymeBtn -> url = "https://rhymebrain.com/talk?function=getRhymes&word=$inputText"
        }

        try {
            getDataFromServer(url, view)

        } catch (e: IOException) {
            Snackbar.make(recyclerView, "Error getting data. Please try again.", Snackbar.LENGTH_LONG).show()
        }
    }

    private fun getDataFromServer(url: String, view: View) {
        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()
        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                Snackbar.make(recyclerView, "Error getting data. Please try again.", Snackbar.LENGTH_LONG).show()
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (response.code == 404) {
                        resultsTitle.text = "No ${currentBtn.lowercase()} found."

                    } else if (!response.isSuccessful) {
                        throw IOException("Unexpected code $response.code")

                    }  else {
                        when (view.id) {
                            R.id.defBtn -> readJSONDef(response.body!!.string())
                            R.id.synBtn -> readJSONSyn(response.body!!.string())
                            R.id.rhymeBtn -> readJSONRhyme(response.body!!.string())
                        }
                    }
                }
            }
        })
    }

    private fun readJSONRhyme(rawJson: String) {
        runOnUiThread(java.lang.Runnable {
            try {
                val json = JSONArray(rawJson)
                val rhymeList = mutableListOf<String>()

                for (i in 0 until json.length()) {
                    val obj = json.getJSONObject(i)
                    val word = obj.getString("word")
                    rhymeList.add(word)
                }

                resultsTitle.text = currentWord
                updateRecycler(rhymeList, numColumns = 3)

            } catch (e: IOException) {
                resultsTitle.text = "Error"
                Snackbar.make(recyclerView, "Error getting rhymes. Please try again.", Snackbar.LENGTH_LONG).show()
            }
        })
    }

    private fun readJSONDef(rawJson: String) {
        runOnUiThread(java.lang.Runnable {
            try {
                val json = JSONArray(rawJson)
                val defList = mutableListOf<String>()
                for (i in 0 until json.length()) {
                    val meanings = json.getJSONObject(i).getJSONArray("meanings")

                    for (j in 0 until meanings.length()) {
                        val partOfSpeech = meanings.getJSONObject(j).getString("partOfSpeech")
                        val definitions = meanings.getJSONObject(j).getJSONArray("definitions")

                        for (k in 0 until definitions.length()) {
                            defList.add("<b><i>$partOfSpeech</i></b>&nbsp;&nbsp;${definitions.getJSONObject(k).getString("definition")}")
                        }
                    }
                }

                resultsTitle.text = currentWord
                updateRecycler(defList, numColumns = 1)

            } catch (e: JSONException) {
                resultsTitle.text = "Error"
                Snackbar.make(recyclerView, "Error getting definitions. Please try again.", Snackbar.LENGTH_LONG).show()
            }
        })
    }

    private fun readJSONSyn(rawJson: String) {
        runOnUiThread(java.lang.Runnable {
            try {
                val json = JSONArray(rawJson)
                val synonyms = mutableListOf<String>()

                if (json.length() > 0 && json.get(0) is String) {
                    for (i in 0 until json.length()) {
                        synonyms.add(json.getString(i))
                    }

                } else if (json.length() > 0 && json.get(0) is JSONObject) {
                    for (i in 0 until json.length()) {
                        val meta = json.getJSONObject(i).getJSONObject("meta")
                        val synsArr1 = meta.getJSONArray("syns")

                        for (j in 0 until synsArr1.length()) {
                            val synsArr2 = synsArr1.getJSONArray(j)

                            for (k in 0 until synsArr2.length()) {
                                synonyms.add(synsArr2.getString(k).toString())
                            }
                        }
                    }
                }

                resultsTitle.text = currentWord
                updateRecycler(synonyms, numColumns = 3)

            } catch (e: IOException) {
                resultsTitle.text = "Error"
                Snackbar.make(recyclerView, "Error getting rhymes. Please try again.", Snackbar.LENGTH_LONG).show()
            }
        })
    }

    private fun resetBtnColours() {
        defBtn.setBackgroundColor(defaultBtnColour)
        synBtn.setBackgroundColor(defaultBtnColour)
        rhymeBtn.setBackgroundColor(defaultBtnColour)
    }

    private fun updateRecycler(items: MutableList<String>, numColumns: Int = 1) {
        // set the number of columns of the layout
        layoutManager.spanCount = numColumns

        // empty all existing items in list - should already be done but jic
        recyclerItemList.clear()

        // add all items from the "items" param to the recyclerItemList
        recyclerItemList.addAll(items)

        // confirm that dataset has changed
        dictAdapter.notifyDataSetChanged()
    }
}