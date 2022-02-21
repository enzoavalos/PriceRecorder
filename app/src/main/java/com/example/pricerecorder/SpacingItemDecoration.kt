package com.example.pricerecorder

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

/* RecyclerView.ItemDecoration its a class that allows to add a special design or transition to the views of the
adapter. In this case, it adds a spacing between list elements*/
class SpacingItemDecoration(private val padding:Int):RecyclerView.ItemDecoration() {
    override fun getItemOffsets(
        outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State)
    {
        super.getItemOffsets(outRect, view, parent, state)
        padding.also {
            outRect.top = it
            outRect.bottom = it
            outRect.left = it
            outRect.right = it
        }
    }
}