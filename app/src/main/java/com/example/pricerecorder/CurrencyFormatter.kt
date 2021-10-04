package com.example.pricerecorder

import android.widget.EditText
import com.example.pricerecorder.addFragment.AddFragment

class CurrencyFormatter {
    companion object{
        //Formates the price input
        fun formatInput(editText: EditText){
            var sequence:String = editText.text.toString()
            var cursorPosition = editText.selectionStart

            if(sequence.isNotEmpty()){
                if((!sequence.contains('.')) and (sequence.length > AddFragment.MAX_INTEGRAL_DIGITS)) {
                    sequence = sequence.dropLast(1)
                    cursorPosition -= 1
                }

                if(sequence.startsWith(".")) {
                    sequence = "0$sequence"
                    cursorPosition += 1
                }

                var pattern = Regex("^0[0-9]")
                if(sequence.contains(pattern)) {
                    sequence = sequence.dropWhile { it == '0' }
                    if(sequence.isEmpty()) {
                        sequence = "0"
                        cursorPosition = 1
                    }else{
                        cursorPosition = sequence.length
                    }
                }

                pattern = Regex("\\..[0-9]+")
                if(sequence.contains(pattern)) {
                    sequence = sequence.dropLast(1)
                    cursorPosition-=1
                }

                editText.setText(sequence)
                editText.setSelection(cursorPosition)
            }
        }
    }
}