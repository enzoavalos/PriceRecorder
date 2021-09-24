package com.example.pricerecorder

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.pricerecorder.database.Product
import com.example.pricerecorder.databinding.ListItemProductBinding

/*Adapta los datos usados a la recyclerView*/
class ProductAdapter(private val clickListener: ProductListener): ListAdapter<Product,RecyclerView.ViewHolder>(ProductDiffCallback) {

    /*Crea los distintos viewholders usados, sera llamado cuando no haya viewholders existentes para reutilizar*/
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return ProductViewHolder.from(parent)
    }

    /*Metodo llamado para vincular las vistas no usadas con nuevos objetos y reciclarlas*/
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when(holder){
            is ProductViewHolder -> {
                holder.bind(getItem(position),clickListener)
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
    class ProductViewHolder private constructor(private val binding: ListItemProductBinding): RecyclerView.ViewHolder(binding.root){
        /*Responsable de unir cada objeto Producto a las vistas del layout*/
        fun bind(product: Product,clickListener: ProductListener){
            binding.product = product
            binding.clickListener = clickListener
            binding.executePendingBindings()
        }

        //Metodo de una clase que puede ser llamado sin necesidad de crear una instancia de la clase
        companion object{
            fun from(parent: ViewGroup): RecyclerView.ViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ListItemProductBinding.inflate(layoutInflater,parent,false)
                return ProductViewHolder(binding)
            }
        }
    }
}

/*Clase que implementa un listener para cuando el usuario clickea sobre un elemento de la lista.
* Recibe como parametro una expresion lambda.*/
class ProductListener(val clickListener: (product:Product) -> Unit){
    fun onClick(product: Product) = clickListener(product)
}