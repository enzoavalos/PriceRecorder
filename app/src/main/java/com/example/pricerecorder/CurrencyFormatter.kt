package com.example.pricerecorder

import android.widget.EditText
import com.example.pricerecorder.addFragment.AddFragment

/*Class responsible for formatting a text numeric input into a valid currency format*/
class CurrencyFormatter {
    companion object{
        fun formatInput(editText: EditText){
            var sequence:String = editText.text.toString()
            var cursorPosition = editText.selectionStart

            if(sequence.isNotEmpty()){
                //Allows only 5 integral digits
                if((!sequence.contains('.')) and (sequence.length > AddFragment.MAX_INTEGRAL_DIGITS)) {
                    sequence = sequence.dropLast(1)
                    cursorPosition -= 1
                }

                //Adds a 0 to the start if the price starts with a "."
                if(sequence.startsWith(".")) {
                    sequence = "0$sequence"
                    cursorPosition += 1
                }

                //Does not allow numbers starting with 0
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

                //Allows only 2 decimal digits
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