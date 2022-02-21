package com.example.pricerecorder

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.pricerecorder.database.Product
import com.example.pricerecorder.databinding.ListItemProductBinding

/* Adapts the data classes used by the program to the recyclerView*/
class ProductAdapter(private val clickListener: ProductListener): ListAdapter<Product,RecyclerView.ViewHolder>(ProductDiffCallback) {

    /* Creates the different view holders used. It will be called when there are no existing view holders to be reused*/
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return ProductViewHolder.from(parent)
    }

    /* This method is called to bind the unused views to new objects in order to recycle them*/
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when(holder){
            is ProductViewHolder -> {
                holder.bind(getItem(position),clickListener)
            }
        }
    }

    //Returns the object bound to the given view holder
    fun getItemProduct(viewHolder: ProductViewHolder) : Product?{
        return getItem(viewHolder.bindingAdapterPosition)
    }

    /* This object allows to identify the changes made between 2 lists, and returns a series of minimal operations
    needed to convert the first list into the second one in an efficient way*/
    object ProductDiffCallback : DiffUtil.ItemCallback<Product>(){
        override fun areItemsTheSame(oldItem: Product, newItem: Product): Boolean {
            return oldItem.productId == newItem.productId
        }

        override fun areContentsTheSame(oldItem: Product, newItem: Product): Boolean {
            return oldItem == newItem
        }
    }

    /* This class extends RecyclerView.ViewHolder because that is the type of view holder that was specified to the adapter*/
    class ProductViewHolder private constructor(private val binding: ListItemProductBinding): RecyclerView.ViewHolder(binding.root){
        /* Responsible of binding each Product object to the views of the layout*/
        fun bind(product: Product,clickListener: ProductListener){
            binding.product = product
            binding.clickListener = clickListener
            if(binding.product!!.image != null)
                binding.productImage.setImageBitmap(binding.product!!.image)
            binding.executePendingBindings()
        }

        /*Method of a class that can be called without the need of instantiating the class*/
        companion object{
            fun from(parent: ViewGroup): RecyclerView.ViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ListItemProductBinding.inflate(layoutInflater,parent,false)
                return ProductViewHolder(binding)
            }
        }
    }
}

/* This class implements an onClick listener for when the user clicks on an element of the list.
 Receives a lambda expression as a parameter*/
class ProductListener(val clickListener: (product:Product) -> Unit){
    fun onClick(product: Product) = clickListener(product)
}