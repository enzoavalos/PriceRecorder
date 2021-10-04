package com.example.pricerecorder

import android.widget.EditText
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

//Enables accept button when all editTexts inputs are valid
fun MaterialButton.setAcceptButtonEnabled(enabled:Boolean){
    this.isEnabled = enabled
    alpha = if(enabled)
        1F
    else
        0.7F
}

fun EditText.validateNumericInputDouble() : Boolean{
    if(!this.text.isNullOrEmpty()){
        try{
            this.text.toString().toDouble()
            return true
        }catch (e:Exception){
            this.error = "Valor Invalido"
        }
    }
    return false
}

fun TextInputEditText.validateTextInput() : Boolean{
    return !this.text.isNullOrEmpty()
}