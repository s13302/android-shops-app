package s13302.pl.shops.repository

interface DataListener<T> {

    fun onDataChange(data: List<T>)

}