package hm.moe.pokkedoll.warscore.ui

import hm.moe.pokkedoll.warscore.{WarsCore, WarsCoreAPI}
import hm.moe.pokkedoll.warscore.utils.{ItemUtil, ShopUtil}
import net.md_5.bungee.api.ChatColor
import org.bukkit.{Bukkit, Material, NamespacedKey, Sound}
import org.bukkit.entity.Player
import org.bukkit.event.inventory.{InventoryClickEvent, InventoryType}
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

import scala.jdk.CollectionConverters._

object ShopUI {

  val TITLE: String => String = (name: String) => s"Shop: $name"

  val EMPTY = new ItemStack(Material.BARRIER)

  val shopIdKey = new NamespacedKey(WarsCore.instance, "shop-id")
  val shopIndexKey = new NamespacedKey(WarsCore.instance, "shop-index")

  def openShopUI(player: Player, name: String): Unit = {
    val shops = ShopUtil.getShops(name)
    val inv = Bukkit.createInventory(null, 54, TITLE(name))
    shops.indices.foreach(i => {
      val shop = shops(i)

      // ストレージ(weapon)内でのアイテム
      val storage = WarsCore.instance.database.getRequireItemsAmount(player.getUniqueId.toString, shop.price)
      // 性善説
      var buyable = true

      val product = ItemUtil.getItem(shop.product.name).getOrElse(EMPTY).clone()
      val productMeta = product.getItemMeta
      productMeta.setDisplayName(productMeta.getDisplayName + " × " + shop.product.amount)
      productMeta.setLore(
        (List(
          "§f-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-",
          "§aクリックして購入する！",
          "§f-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-",
          "§7§l| §7要求アイテム §7§l: §7必要数 §8/ §7所持数") ++
          shop.price.map(f => {
            WarsCoreAPI.getItemStackName(ItemUtil.getItem(f.name).getOrElse(EMPTY)) + "§f: " +
              {
                val rAmount = storage.getOrElse(f.name, 0)
                if(rAmount >= f.amount) {
                  f.amount + " / " + rAmount
                } else {
                  if(buyable) buyable = false
                  ChatColor.RED + "" + f.amount + " / " + rAmount
                }
              }
          }).toList ++
          List(
            "§7§l|",
            "§7§l| §7達成条件 §7§l: §7必要値 §8/ §7現在値",
            // "§fテスト条件: §c0 / 1"
          )
          ).asJava)
      if(buyable) {
        productMeta.getPersistentDataContainer.set(shopIdKey, PersistentDataType.STRING, name)
        productMeta.getPersistentDataContainer.set(shopIndexKey, PersistentDataType.INTEGER, Integer.valueOf(i))
      }
      product.setItemMeta(productMeta)
      inv.setItem(9 + i, product)
    })
    player.openInventory(inv)
  }

  def onClick(e: InventoryClickEvent): Unit = {
    e.setCancelled(true)
    e.getClickedInventory.getType match {
      case InventoryType.CHEST =>
        val slot = e.getSlot
        val item = e.getCurrentItem
        val player = e.getWhoClicked.asInstanceOf[Player]
        if(item != null && item.getType != Material.AIR && item.hasItemMeta) {
          val data = item.getItemMeta.getPersistentDataContainer
          if(data.has(shopIdKey, PersistentDataType.STRING) && data.has(shopIndexKey, PersistentDataType.INTEGER)) {
            val shop = ShopUtil.getShops(data.get(shopIdKey, PersistentDataType.STRING))(data.get(shopIndexKey, PersistentDataType.INTEGER))
            WarsCoreAPI.debug(player, "購入処理！")
            WarsCore.instance.database.addWeapon(player.getUniqueId.toString, shop.`type`, shop.product.name, shop.product.amount)
            WarsCore.instance.database.delWeapon(player.getUniqueId.toString, shop.price)
            player.playSound(player.getLocation, Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 1.5f)
            openShopUI(player, e.getView.getTitle.replaceAll("Shop: ", ""))
          } else {
            WarsCoreAPI.debug(player, "何も起きず！")
          }
        }
      case _ =>
    }
  }
}
