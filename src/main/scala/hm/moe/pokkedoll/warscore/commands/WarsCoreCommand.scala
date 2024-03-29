package hm.moe.pokkedoll.warscore.commands

import com.google.common.io.ByteStreams
import hm.moe.pokkedoll.warscore.db.WeaponDB
import hm.moe.pokkedoll.warscore.ui.{SndCheckerUI, WeaponUI}
import hm.moe.pokkedoll.warscore.utils.{BossBarMessage, Item, ItemUtil, TagUtil}
import hm.moe.pokkedoll.warscore.{WarsCore, WarsCoreAPI}
import hm.moe.pokkedoll.warsgame.Skills
import net.md_5.bungee.api.chat.ComponentBuilder
import org.bukkit.{Bukkit, ChatColor, Location}
import org.bukkit.command.{Command, CommandExecutor, CommandSender}
import org.bukkit.entity.{ArmorStand, EntityType, Player}
import org.bukkit.inventory.{Inventory, ItemStack}
import org.bukkit.metadata.FixedMetadataValue
import org.bukkit.persistence.PersistentDataType
import org.bukkit.scheduler.BukkitRunnable

class WarsCoreCommand extends CommandExecutor {

  val t = new Array[Inventory](3)

  override def onCommand(sender: CommandSender, command: Command, label: String, args: Array[String]): Boolean = {
    sender match {
      case player: Player =>
        if (args.length == 0) {
          player.sendMessage(
            ChatColor.translateAlternateColorCodes('&',
              "HELP\n" +
                "/whelp [economy|eco] [get] [ingot|coin] [amount]\n" +
                "* 自身にぽっけコインを与えます"
            )
          )
        } else {
          args(0) match {
            case "reload" | "r" =>
              if(args.length > 1) {
                args(1) match {
                  case "item" =>
                    ItemUtil.reloadItem()
                  case "tag" =>
                    TagUtil.init()
                  case "default" =>
                    WarsCore.instance.reloadConfig()
                    // WarsCoreAPI.reloadMapInfo(WarsCore.instance.getConfig.getConfigurationSection("mapinfo"))
                }
              } else {
                sender.sendMessage(
                  "= = = = = = = = /wc reload ... = = = = = = = =\n" +
                    "* default: 通常のconfigをリロードする\n" +
                    "* item: アイテム情報をリロードする\n" +
                    "* merchant: 取引情報をリロードする\n" +
                    "* tag: タグ情報をリロードする\n" +
                    "* cspp: CSPP情報をリロードする"
                )
              }
            case "createweapon" | "cw" =>
              if(args.length > 2 && WeaponDB.is(args(2))) {
                ItemUtil.getItem(args(1)) match {
                  case Some(item) =>
                    val result = item.clone()
                    val meta = result.getItemMeta
                    meta.setDisplayName("テキスト見本(renameなりなんなり)")
                    meta.setLore(java.util.Arrays.asList(ChatColor.GRAY + "【" +ChatColor.DARK_AQUA + "クリック"+ChatColor.GRAY+"】"+ChatColor.YELLOW+"アイテムを獲得します"))
                    meta.getPersistentDataContainer.set(WarsCoreAPI.weaponUnlockNameKey, PersistentDataType.STRING, args(1))
                    meta.getPersistentDataContainer.set(WarsCoreAPI.weaponUnlockTypeKey, PersistentDataType.STRING, args(2))
                    result.setItemMeta(meta)
                    player.getInventory.addItem(result)
                  case _ =>
                }
              } else {
                player.sendMessage(
                  "= = = = = = = = /wc createweapon ... = = = = = = = =\n" +
                  ""
                )
              }
            case "test" | "t" =>
              if(args.length > 1) {
                args(1) match {
                  case "s" =>
                    val i = player.getInventory.getItemInMainHand
                    player.sendMessage(s"ITEM => $i\nBytes => ${i.serializeAsBytes().mkString("Array(", ", ", ")")}")
                    player.setMetadata("i", new FixedMetadataValue(WarsCore.instance, i.serializeAsBytes()))
                  case "g" =>
                    val b = player.getMetadata("i").get(0)
                    val i = ItemStack.deserializeBytes(b.value().asInstanceOf[Array[Byte]])
                    player.getInventory.addItem(i)
                  case "tags" =>
                    val comp = new ComponentBuilder()
                    TagUtil.cache.foreach(f => comp.append(s"${f._1}: ${f._2}\n"))
                    player.sendMessage(comp.create():_*)
                  case "addWeapon" =>
                    WarsCore.instance.database.addWeapon(player.getUniqueId.toString, args(2), args(3), args(4).toInt)
                  case "delWeapon" =>
                    WarsCore.instance.database.delWeapon(player.getUniqueId.toString, Array(new Item("coin", 10), new Item("ak-47", 1)))
                  case _ =>
                }
              } else {
                player.sendMessage("エラー！")
              }
            case "vp" =>
              val out = ByteStreams.newDataOutput
              out.writeUTF("TakeVotePoint")
              player.sendPluginMessage(WarsCore.instance, WarsCore.MODERN_TORUS_CHANNEL, out.toByteArray)
            case "rs" =>
              val out = ByteStreams.newDataOutput
              out.writeUTF("ResourcePack")
              player.sendPluginMessage(WarsCore.instance, WarsCore.MODERN_TORUS_CHANNEL, out.toByteArray)
            case "cmdesp" =>
              if (player.hasMetadata("cmdesp")) {
                player.removeMetadata("cmdesp", WarsCore.instance)
                player.sendMessage(ChatColor.BLUE + "CMDESPを無効化しました")
              } else {
                player.setMetadata("cmdesp", new FixedMetadataValue(WarsCore.instance, "true"))
                player.sendMessage(ChatColor.BLUE + "CMDESPを有効化しました")
              }
            case "changedisplayname" | "cd" =>
              if (args.length > 1) {
                val entities = player.getNearbyEntities(1d, 1d, 1d).stream().filter(p => p.getType != EntityType.PLAYER).findFirst()
                entities.ifPresent(t => {
                  player.sendMessage(s"${t.getLocation.toString}のエンティティの名前を変更しました。")
                  t.setCustomNameVisible(true)
                  t.setCustomName(ChatColor.translateAlternateColorCodes('&', args.tail.mkString(" ")))
                })
              }
            case "sndChecker" | "sc" =>
              SndCheckerUI.openUI(player)
            case "storage" =>
              WeaponUI.openStorageUI(player, 1)
            case "bossbar" =>
              BossBarMessage.send("テスト(10秒)", 10)
            case "damage" =>
              player.damage(100.0)
            case "task" => {
              def o(a: Unit): BukkitRunnable = {
                () => {
                  a
                }
              }

              val a = o({
                player.sendMessage("a")
                player.sendMessage("a")
                player.sendMessage("a")
                player.sendMessage("a")
                player.sendMessage("a")
                player.sendMessage("a")
              })

              a.runTask(WarsCore.instance)

            }
            case "sk:bh" =>
              Skills.bh(player)
            case _ =>
          }
        }
    }
    true
  }
}
