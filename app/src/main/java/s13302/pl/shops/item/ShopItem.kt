package s13302.pl.shops.item

import com.squareup.picasso.Picasso
import com.xwray.groupie.Item
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.shop_row.view.*
import s13302.pl.shops.R
import s13302.pl.shops.data.Shop

class ShopItem(val shop: Shop): Item<ViewHolder>() {

    override fun getLayout(): Int {
        return R.layout.shop_row
    }

    override fun bind(viewHolder: ViewHolder, position: Int) {
        Picasso.get().load(shop.photoUri).into(viewHolder.itemView.ciShopImage)
        viewHolder.itemView.tvShopName.text = shop.name
        viewHolder.itemView.tvShopType.text = shop.type
    }

}