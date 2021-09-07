package com.example.pricerecorder

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.pricerecorder.database.Product
import com.example.pricerecorder.generated.callback.OnClickListener

/*Adapta los datos usados a la recyclerView*/
class ProductAdapter: ListAdapter<Product,RecyclerView.ViewHolder>(ProductDiffCallback) {

    /*Crea los distintos viewholders usados, sera llamado cuando no haya viewholders existentes para reutilizar*/
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return ProductViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.list_item_product,parent,false)
        )
    }

    /*Metodo llamado para vincular las vistas no usadas con nuevos objetos y reciclarlas*/
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when(holder){
            is ProductViewHolder -> {
                holder.bind(getItem(position))
            }
        }
    }

    /*Este objeto permite identificar los cambios entre 2 listas, y retorna una serie de operaciones minimas para
    * para convertir la 1er lista en la segunda de forma eficiente*/
    object ProductDiffCallback : DiffUtil.ItemCallback<Product>(){
        override fun areItemsTheSame(oldItem: Product, newItem: Product): Boolean {
            return oldItem.productId == newItem.productId
        }

        override fun areContentsTheSame(oldItem: Product, newItem: Product): Boolean {
            return oldItem == newItem
        }
    }

    /*La clase extiende de RecyclerView.ViewHolder ya que ese es el tipo de viewholder que se le especifico al adapter.*/
    class ProductViewHolder constructor(itemView:View): RecyclerView.ViewHolder(itemView){
        private val description:TextView = itemView.findViewById(R.id.product_description_text)
        private val purchasePlace:TextView = itemView.findViewById(R.id.purchase_place_text)
        private val price:TextView = itemView.findViewById(R.id.product_price_text)
        private val updateDate:TextView = itemView.findViewById(R.id.update_date_text)
        private val productImage:ImageView = itemView.findViewById(R.id.product_image)

        /*Responsable de unir cada objeto Producto a las vistas del layout*/
        fun bind(product: Product){
            description.text = product.description
            purchasePlace.text = product.placeOfPurchase
            price.text = product.price.toString()
            updateDate.text = product.updateDate
            productImage.setImageResource(R.drawable.ic_broken_image)
        }
    }
}

/*Clase que implementa un click listener para los elementos de la lista*/
class ProductListener(val clickListener: (productId :Long) -> Unit){
    fun onClick(product: Product) = product.productId
}