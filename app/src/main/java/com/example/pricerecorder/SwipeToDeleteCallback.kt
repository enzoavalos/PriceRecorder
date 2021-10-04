package com.example.pricerecorder

import android.content.Context
import android.graphics.Canvas
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView

import androidx.core.content.ContextCompat
import it.xabaras.android.recyclerview.swipedecorator.RecyclerViewSwipeDecorator

//Extends utility class ItemTouchHelper.SimpleCallback used to add swipe and drag support to Recycler View
abstract class SwipeToDeleteCallback(context: Context) : ItemTouchHelper.SimpleCallback(0,ItemTouchHelper.LEFT) {
    private val backgroundColor = ContextCompat.getColor(context,R.color.red)

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        return false
    }

    //Controls how the view responds to user interaction. By default ItemTouchHelper moves the item applying
    //a translation leaving an empty background, with this method that behaviour can be customized
    override fun onChildDraw(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        RecyclerViewSwipeDecorator.Builder(c,recyclerView,viewHolder,dX,
            dY,actionState,isCurrentlyActive)
            .addBackgroundColor(backgroundColor)
            .addActionIcon(R.drawable.ic_delete)
            .setSwipeLeftActionIconTint(R.color.white)
            .create()
            .decorate()

        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
    }
}