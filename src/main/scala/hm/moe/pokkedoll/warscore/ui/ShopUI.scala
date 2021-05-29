package hm.moe.pokkedoll.warscore.ui

import hm.moe.pokkedoll.warscore.{Test, WarsCore, WarsCoreAPI}
import hm.moe.pokkedoll.warscore.utils.{ItemUtil, ShopUtil}
import net.md_5.bungee.api.ChatColor
import org.bukkit.{Bukkit, Material, NamespacedKey, Sound}
import org.bukkit.entity.Player
import org.bukkit.event.inventory.{InventoryClickEvent, InventoryType}
import org.bukkit.inventory.{Inventory, InventoryView, ItemStack}
import org.bukkit.persistence.PersistentDataType

import scala.jdk.CollectionConverters._

object ShopUI {

  val TITLE: String => String = (name: String) => s"Shop: $name"

  val EMPTY = new ItemStack(Material.BARRIER)

  val shopIdKey = new NamespacedKey(WarsCore.instance, "shop-id")
  val shopIndexKey = new NamespacedKey(WarsCore.instance, "shop-index")

  private val topAndBottomIcon = {
    val i = new ItemStack(Material.LIME_STAINED_GLASS_PANE)
    val m = i.getItemMeta
    m.setDisplayName(" ")
    i.setItemMeta(m)
    i
  }

  private val contentIcon = {
    val i = new ItemStack(Material.BLUE_STAINED_GLASS_PANE)
    val m = i.getItemMeta
    m.setDisplayName(" ")
    i.setItemMeta(m)
    i
  }

  // TODO もっとエコにできるかもしれない。
  // TODO まずい。あまりにも汚い。
  def openShopUI(player: Player, name: String, offset: Int = 0): Unit = {
    val test = new Test("openShopUI > v1.9.5")
    val shops = ShopUtil.getShops(name)
    val inv = Bukkit.createInventory(null, 45, TITLE(name))

    val groundwork = Array.fill(9)(topAndBottomIcon) ++ Array.fill(27)(contentIcon) ++ Array.fill(9)(topAndBottomIcon)

    groundwork.update(0, WarsCoreAPI.UI.CLOSE)
    groundwork.update(18, {
      val i = new ItemStack(Material.LADDER)
      val m = i.getItemMeta
      if (offset == 0) {
        m.setDisplayName(ChatColor.RED + "-")
      } else {
        m.setDisplayName(ChatColor.WHITE + "←")
        m.getPersistentDataContainer.set(shopIdKey, PersistentDataType.STRING, name)
        m.getPersistentDataContainer.set(WarsCoreAPI.UI.PAGE_KEY, PersistentDataType.INTEGER, Integer.valueOf(offset - 9))
      }
      i.setItemMeta(m)
      i
    })
    groundwork.update(26, {
      val i = new ItemStack(Material.LADDER)
      val m = i.getItemMeta
      if (offset > shops.length) {
        m.setDisplayName(ChatColor.RED + "-")
      } else {
        m.setDisplayName(ChatColor.WHITE + "→")
        m.getPersistentDataContainer.set(shopIdKey, PersistentDataType.STRING, name)
        m.getPersistentDataContainer.set(WarsCoreAPI.UI.PAGE_KEY, PersistentDataType.INTEGER, Integer.valueOf(offset + 9))
      }
      i.setItemMeta(m)
      i
    })

    val slicedShop = shops.slice(offset, offset + 9)
    var index = 10

    slicedShop.indices.foreach(i => {
      val shop = slicedShop(i)
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
            WarsCoreAPI.getItemStackName(ItemUtil.getItem(f.name).getOrElse(EMPTY)) + "§f: " + {
              val rAmount = storage.getOrElse(f.name, 0)
              if (rAmount >= f.amount) {
                f.amount + " / " + rAmount
              } else {
                if (buyable) buyable = false
                ChatColor.RED + "" + f.amount + " / " + rAmount
              }
            }
          }).toList ++
          List(
            "§7§l|",
            "§7§l| §7達成条件 §7§l: §7必要値 §8/ §7現在値",
          ) ++
          (if (shop.rank != -1 && shop.rank != 0) {
            List({
              val rank = WarsCoreAPI.getWPlayer(player).rank
              "§fランク: " + (if (rank >= shop.rank) {
                shop.rank + " / " + rank
              } else {
                if(buyable) buyable = false
                ChatColor.RED + "" + shop.rank + " / " + rank
              })})
          } else {
            List.empty[String]
          })
          ).asJava)
      if (buyable) {
        productMeta.getPersistentDataContainer.set(shopIdKey, PersistentDataType.STRING, name)
        productMeta.getPersistentDataContainer.set(shopIndexKey, PersistentDataType.INTEGER, Integer.valueOf(offset + i))
      }
      product.setItemMeta(productMeta)
      groundwork.update(index, product)

      if (index == 16) index = 28 else index += 2
    })

    inv.setContents(groundwork)
    player.openInventory(inv)
    test.log(1L)
  }
  


  // TODO どちらかというとoffsetKey
  // TODO どのアイコンにおいてもoffset, nameは使用している。統一したほうが良いのでは？
  def onClick(player: Player, inv: Inventory, slot: Int, item: ItemStack, view: InventoryView): Unit = {
    inv.getType match {
      case InventoryType.CHEST =>
        if (item != null && item.getType != Material.AIR && item.hasItemMeta) {
          val data = item.getItemMeta.getPersistentDataContainer
          if (slot == 18 && data.has(WarsCoreAPI.UI.PAGE_KEY, PersistentDataType.INTEGER)) {
            openShopUI(player, data.get(shopIdKey, PersistentDataType.STRING), data.get(WarsCoreAPI.UI.PAGE_KEY, PersistentDataType.INTEGER))
          } else if (slot == 26 && data.has(WarsCoreAPI.UI.PAGE_KEY, PersistentDataType.INTEGER)) {
            openShopUI(player, data.get(shopIdKey, PersistentDataType.STRING), data.get(WarsCoreAPI.UI.PAGE_KEY, PersistentDataType.INTEGER))
          } else if (data.has(shopIdKey, PersistentDataType.STRING) && data.has(shopIndexKey, PersistentDataType.INTEGER)) {
            val shop = ShopUtil.getShops(data.get(shopIdKey, PersistentDataType.STRING))(data.get(shopIndexKey, PersistentDataType.INTEGER))
            // WarsCoreAPI.debug(player, "購入処理！")
            WarsCore.instance.database.addWeapon(player.getUniqueId.toString, shop.`type`, shop.product.name, shop.product.amount)
            WarsCore.instance.database.delWeapon(player.getUniqueId.toString, shop.price)
            player.playSound(player.getLocation, Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 1.5f)
            openShopUI(player, view.getTitle.replaceAll("Shop: ", ""))
            // キャッシュのクリア
            WeaponUI.weaponCache.remove(player)
          } else {
            // WarsCoreAPI.debug(player, "何も起きず！")
            player.playSound(player.getLocation, Sound.ITEM_ARMOR_EQUIP_GENERIC, 1f, 1f)
          }
        }
      case _ =>
    }
  }
}
