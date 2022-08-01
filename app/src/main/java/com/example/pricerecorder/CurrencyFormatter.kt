package com.example.pricerecorder

import com.example.pricerecorder.addFragment.AddFragment

/*Class responsible for formatting a text numeric input into a valid currency format*/
class CurrencyFormatter {
    companion object{
        fun isInputNumericValid(input:String):Boolean{
            if(input.isEmpty())
                return true
            return !input.startsWith('.') and (input.toDoubleOrNull() != null)
        }

        fun formatInput(input:String):String{
            var output = input

            if(output.isNotEmpty()){
                //Does not allow numbers with a 0 on the leftmost side of the integral part
                var pattern = Regex("^0[0-9]")
                if(output.contains(pattern)) {
                    output = output.dropWhile { it == '0' }
                    if(output.isEmpty())
                        output = "0"
                }

                //Allows only 2 decimal digits
                pattern = Regex("\\..[0-9]+")
                if(output.contains(pattern))
                    output = output.dropLast(1)

                //Allows only 6 integral digits
                var (integrals,decimals,separator) = if(output.contains('.'))
                    listOf(output.substringBefore('.'),output.substringAfter('.'),".")
                    else listOf(output,"","")

                if(integrals.isNotEmpty() and (integrals.length > AddFragment.MAX_INTEGRAL_DIGITS))
                    integrals = integrals.dropLast(1)

                output = "$integrals$separator$decimals"
            }
            return output
        }
    }
}