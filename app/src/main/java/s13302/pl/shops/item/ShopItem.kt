package s13302.pl.shops.item

import android.content.Intent
import android.util.Log
import com.squareup.picasso.Picasso
import com.xwray.groupie.Item
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.shop_row.view.*
import s13302.pl.shops.ProjectConstants
import s13302.pl.shops.R
import s13302.pl.shops.data.Shop
import s13302.pl.shops.ui.EditShopActivity

class ShopItem(val shop: Shop): Item<ViewHolder>() {

    override fun getLayout(): Int {
        return R.layout.shop_row
    }

    override fun bind(viewHolder: ViewHolder, position: Int) {
        Picasso.get().load(shop.photoUri).into(viewHolder.itemView.ciShopImage)
        viewHolder.itemView.tvShopName.text = shop.name
        viewHolder.itemView.tvShopType.text = shop.type

        viewHolder.itemView.setOnClickListener {
            Log.d("ShopItem", "Clicked ${shop.name}")
            val context = viewHolder.itemView.context
            val intent = Intent(context, EditShopActivity::class.java)
            intent.putExtra(ProjectConstants.SHOP_EXTRA, shop)
            context.startActivity(intent)
        }
    }

}