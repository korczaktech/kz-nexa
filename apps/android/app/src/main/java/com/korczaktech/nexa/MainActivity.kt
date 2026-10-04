package com.korczaktech.nexa
import android.app.Activity
import android.os.Bundle
import android.widget.TextView
class MainActivity:Activity(){override fun onCreate(state:Bundle?){super.onCreate(state);setContentView(TextView(this).apply{text="Korczak Nexa\n\nFundação pronta — NexaAPI";textSize=24f;setPadding(48,48,48,48)}})}