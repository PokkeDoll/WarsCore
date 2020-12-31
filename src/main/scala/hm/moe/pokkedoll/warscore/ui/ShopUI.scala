package hm.moe.pokkedoll.warscore.ui

import hm.moe.pokkedoll.warscore.{WarsCore, WarsCoreAPI}
import hm.moe.pokkedoll.warscore.utils.{ItemUtil, ShopUtil}
import org.bukkit.{Bukkit, Material}
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import scala.jdk.CollectionConverters._

object ShopUI {

  val TITLE = (name: String) => s"Shop: $name"

  val EMPTY = new ItemStack(Material.BARRIER)

  def openShopUI(player: Player, name: String): Unit = {
    val shops = ShopUtil.getShops(name)
    val inv = Bukkit.createInventory(null, 54, TITLE(name))
    shops.indices.foreach(i => {
      val shop = shops(i)
      val product = ItemUtil.getItem(shop.product.name).getOrElse(EMPTY).clone()
      val productMeta = product.getItemMeta
      productMeta.setDisplayName(productMeta.getDisplayName + " × " + shop.product.amount)
      productMeta.setLore(
        shop.price.map(f => {
          WarsCoreAPI.getItemStackName(ItemUtil.getItem(f.name).getOrElse(EMPTY)) + " × " + f.amount
        }).toList.asJava)
      product.setItemMeta(productMeta)
      inv.setItem(9 + i, product)
    })
    player.openInventory(inv)
  }
}
