package com.example.pricerecorder

import android.widget.EditText
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

/*Responsible for enabling the accept buttons and changing its ui*/
fun MaterialButton.setAcceptButtonEnabled(enabled:Boolean){
    this.isEnabled = enabled
    alpha = if(enabled)
        1F
    else
        0.7F
}

/*Returns true when the editText contains a valid double value greater than 0.0*/
fun EditText.validatePositiveNumericInputDouble() : Boolean{
    if(!this.text.isNullOrEmpty()){
        try{
            val value = this.text.toString().toDouble()
            if(value > 0.0)
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