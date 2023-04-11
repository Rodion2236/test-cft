package com.rodion2236.test_cft

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Paint
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.*
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.rodion2236.test_cft.data.DataBase
import com.rodion2236.test_cft.data.DataBaseItem
import com.rodion2236.test_cft.databinding.ActivityMainBinding
import com.rodion2236.test_cft.model.CardModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var item: CardModel
    private var requests: Array<String> = emptyArray()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        requestCardData(DEFAULT_BIN)

        updateRequestList()
        updateInformation()

        binding.tvTextLatitude.paintFlags = Paint.UNDERLINE_TEXT_FLAG
        binding.tvTextLongitude.paintFlags = Paint.UNDERLINE_TEXT_FLAG
    }

    companion object {
        private const val URL = "https://lookup.binlist.net/"
        private const val DEFAULT_BIN = "45717360"
        private const val JSON_KEY_BANK = "bank"
        private const val JSON_KEY_COUNTRY = "country"
        private const val JSON_KEY_NUMBER = "number"
        private const val EMPTY_STRING_VALUE = ""
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun updateRequestList() {
        lifecycleScope.launch {
            requests = DataBase.getDb(this@MainActivity).getDao().requestList()
            val adapter: ArrayAdapter<String> = ArrayAdapter(
                this@MainActivity,
                android.R.layout.simple_dropdown_item_1line,
                requests.toList()
            )
            with(binding) {
                etInput.setAdapter(adapter)
                etInput.threshold = 1
                etInput.setOnTouchListener { _, _ ->
                    when {
                        requests.isNotEmpty() -> {
                            when {
                                etInput.text.toString() != EMPTY_STRING_VALUE -> adapter.filter
                                    .filter(null)
                            }
                            etInput.showDropDown()
                        }
                    }
                    false
                }
            }
        }
    }

    private fun saveRequests() {
        val text = binding.etInput.text
        val db = DataBase.getDb(this)
        val request = DataBaseItem(
            null,
            text.toString()
        )
        val requestLiveData = DataBase
            .getDb(this)
            .getDao()
            .getItem(text.toString())

        requestLiveData
            .observe(this) { requests ->
                when (binding.etInput.length()) {
                    8 -> if (requests == null) {
                        Thread {
                            db.getDao()
                                .insertItem(request)
                            updateRequestList()
                        }.start()
                    }
                }
            }
    }

    private fun updateInformation() {
        binding.etInput.addTextChangedListener(object : TextWatcher {

            override fun afterTextChanged(bin: Editable) {
                CoroutineScope(Dispatchers.IO).launch {
                    delay(1000)
                    when (binding.etInput.text.toString()) {
                        EMPTY_STRING_VALUE -> requestCardData(DEFAULT_BIN)
                        else -> requestCardData(bin.toString())
                    }
                }
            }

            override fun beforeTextChanged(
                bin: CharSequence, start: Int,
                count: Int, after: Int
            ) {
            }

            override fun onTextChanged(
                bin: CharSequence, start: Int,
                before: Int, count: Int
            ) {
                saveRequests()
            }
        })
    }

    private fun requestCardData(bin: String) {
        val url = URL + bin
        val queue = Volley.newRequestQueue(this)
        val request = StringRequest(
            Request.Method.GET, url, { r ->
                parseCardData(r)
            }, {
                Toast.makeText(
                    this, getString(R.string.error), Toast.LENGTH_SHORT)
                    .show()
            }
        )
        queue.add(request)
    }

    private fun parseCardData(r: String) {
        try {
            val mObject = JSONObject(r)
            item = CardModel(
                getCardData(mObject, JSON_KEY_NUMBER, "length", true),
                getCardData(mObject, JSON_KEY_NUMBER, "luhn", true),
                getCardData(mObject, JSON_KEY_COUNTRY, "numeric", true),
                getCardData(mObject, JSON_KEY_COUNTRY, "alpha2", true),
                getCardData(mObject, JSON_KEY_COUNTRY, "name", true),
                getCardData(mObject, JSON_KEY_COUNTRY, "emoji", true),
                getCardData(mObject, JSON_KEY_COUNTRY, "currency", true),
                getCardData(mObject, JSON_KEY_COUNTRY, "latitude", true),
                getCardData(mObject, JSON_KEY_COUNTRY, "longitude", true),
                getCardData(mObject, JSON_KEY_BANK, "name", true),
                getCardData(mObject, JSON_KEY_BANK, "url", true),
                getCardData(mObject, JSON_KEY_BANK, "phone", true),
                getCardData(mObject, JSON_KEY_BANK, "city", true),
                getCardData(mObject, EMPTY_STRING_VALUE, "scheme", false),
                getCardData(mObject, EMPTY_STRING_VALUE, "type", false),
                getCardData(mObject, EMPTY_STRING_VALUE, "brand", false),
                getCardData(mObject, EMPTY_STRING_VALUE, "prepaid", false),
            )
            with(binding) {
                tvTextCountry.text = getString(
                    R.string.tvTextCountryAlpha2NameCurrency,
                    item.countryAlpha2,
                    item.countryName,
                    item.countryCurrency
                )
                tvTextBank.text = getString(
                    R.string.tvTextBankNameCity,
                    item.bankName,
                    item.bankCity
                )
                tvTextBrand.text = item.brand
                tvTextLength.text = item.length
                tvTextLuhn.text = item.luhn
                tvTextPhone.text = item.bankPhone
                tvTextPrepaid.text = item.prepaid
                tvTextScheme.text = item.scheme
                tvTextType.text = item.type
                tvTextUrl.text = item.bankUrl
                tvTextLatitude.text = item.countryLatitude
                tvTextLongitude.text = item.countryLongitude
            }
        } catch (e: Exception) {
            Toast.makeText(
                this, getString(R.string.error), Toast.LENGTH_SHORT)
                .show()
        }
    }

    private fun getCardData(
        mainObject: JSONObject,
        jsonObject: String,
        jsonString: String,
        getJSON: Boolean
    ): String {
        var validatedData: String
        try {
            when (getJSON) {
                true -> {
                    validatedData = when {
                        mainObject.has(jsonObject) -> {
                            when {
                                mainObject
                                    .getJSONObject(jsonObject)
                                    .has(jsonString) -> {
                                    mainObject.
                                    getJSONObject(jsonObject).
                                    getString(jsonString)
                                }
                                else -> {
                                    EMPTY_STRING_VALUE
                                }
                            }
                        }
                        else -> {
                            EMPTY_STRING_VALUE
                        }
                    }
                }
                false -> {
                    validatedData = when {
                        mainObject.has(jsonString) -> {
                            mainObject.getString(jsonString)
                        }
                        else -> {
                            EMPTY_STRING_VALUE
                        }
                    }
                }
            }
        } catch (ex: Exception) {
            validatedData = EMPTY_STRING_VALUE
            Toast.makeText(this, getString(R.string.error), Toast.LENGTH_SHORT)
                .show()
        }
        when (validatedData) {
            "visa" -> validatedData = "Visa"
            "mastercard" -> validatedData = "Mastercard"
            "debit" -> validatedData = "Debit"
            "credit" -> validatedData = "Credit"
            "true" -> validatedData = "Yes"
            "false" -> validatedData = "No"
        }
        return validatedData
    }

    fun goToMap(view: View) {
        val sLatitude = item.countryLatitude
        val sLongitude = item.countryLongitude
        val url = "geo:$sLatitude,$sLongitude?z=22&q=$sLatitude,$sLongitude"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(intent)
    }
}