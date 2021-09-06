package com.example.pricerecorder

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

/*RecyclerView.ItemDecoration es una clase que permite agregar un dibujo especial o desplazamiento a vistas del
* adaptador. En este caso agerga un espaciado entre los elementos de la lista*/
class SpacingItemDecoration(private val padding:Int):RecyclerView.ItemDecoration() {
    override fun getItemOffsets(
        outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State)
    {
        super.getItemOffsets(outRect, view, parent, state)
        outRect.top = padding
        outRect.bottom = padding
    }
}