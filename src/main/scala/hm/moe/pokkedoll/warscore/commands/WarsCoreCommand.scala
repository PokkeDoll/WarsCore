package hm.moe.pokkedoll.warscore.commands

import com.google.common.io.ByteStreams
import hm.moe.pokkedoll.warscore.db.SQLite
import hm.moe.pokkedoll.warscore.ui.WeaponUI
import hm.moe.pokkedoll.warscore.utils.{ItemUtil, MerchantUtil, TagUtil}
import hm.moe.pokkedoll.warscore.{WarsCore, WarsCoreAPI}
import net.md_5.bungee.api.chat.ComponentBuilder
import org.bukkit.ChatColor
import org.bukkit.command.{Command, CommandExecutor, CommandSender}
import org.bukkit.entity.{EntityType, Player}
import org.bukkit.inventory.{Inventory, ItemStack}
import org.bukkit.metadata.FixedMetadataValue

import scala.collection.mutable

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
          if (args(0).equalsIgnoreCase("reload") || args(0).equalsIgnoreCase("r")) {
            if(args.length > 1) {
              args(1) match {
                case "item" =>
                  ItemUtil.reloadItem()
                case "merchant" =>
                  MerchantUtil.reload()
                case "tag" =>
                  TagUtil.reloadConfig()
                case "cspp" =>
                  WarsCore.instance.setupCSPP()
                case "default" =>
                  WarsCore.instance.reloadConfig()
                  WarsCoreAPI.reloadMapInfo(WarsCore.instance.getConfig.getConfigurationSection("mapinfo"))
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
          } else if (args(0).equalsIgnoreCase("test") || args(0).equalsIgnoreCase("t")) {
            if(args.length > 1) {
              args(1) match {
                case "w" =>
                  if(args(2) == "main") {
                    WeaponUI.openMainUI(player)
                  } else {
                    WeaponUI.openWeaponStorageUI(player)
                  }
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
                case "myset" =>
                  WeaponUI.openMySetUI(player)
                case _ =>
              }
            } else {
              player.sendMessage("エラー！")
            }
          }
          if (args(0) == "vp") {
            val out = ByteStreams.newDataOutput
            out.writeUTF("TakeVotePoint")
            player.sendPluginMessage(WarsCore.instance, WarsCore.MODERN_TORUS_CHANNEL, out.toByteArray)
          } else if (args(0) == "rs") {
            val out = ByteStreams.newDataOutput
            out.writeUTF("ResourcePack")
            player.sendPluginMessage(WarsCore.instance, WarsCore.MODERN_TORUS_CHANNEL, out.toByteArray)
          } else if (args(0) == "cmdesp") {
            if (player.hasMetadata("cmdesp")) {
              player.removeMetadata("cmdesp", WarsCore.instance)
              player.sendMessage(ChatColor.BLUE + "CMDESPを無効化しました")
            } else {
              player.setMetadata("cmdesp", new FixedMetadataValue(WarsCore.instance, "true"))
              player.sendMessage(ChatColor.BLUE + "CMDESPを有効化しました")
            }
          } else if (args(0) == "changedisplayname" || args(0) == "cd") {
            if (args.length > 1) {
              val entities = player.getNearbyEntities(1d, 1d, 1d).stream().filter(p => p.getType != EntityType.PLAYER).findFirst()
              entities.ifPresent(t => {
                player.sendMessage(s"${t.getLocation.toString}のエンティティの名前を変更しました。")
                t.setCustomNameVisible(true)
                t.setCustomName(ChatColor.translateAlternateColorCodes('&', args.tail.mkString(" ")))
              })
            }
          }
        }
    }
    true
  }
}
