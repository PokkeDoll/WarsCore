package hm.moe.pokkedoll.warscore.commands

import hm.moe.pokkedoll.warscore.WarsCore
import hm.moe.pokkedoll.warscore.utils.TagUtil
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.{ClickEvent, ComponentBuilder}
import org.bukkit.Material
import org.bukkit.command.{Command, CommandExecutor, CommandSender}
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.BukkitRunnable

/**
 * タグを設定するコマンド
 *
 * @author Emorard
 * @since 0.21
 */
class TagCommand extends CommandExecutor {

  private val db = WarsCore.instance.database

  override def onCommand(sender: CommandSender, command: Command, label: String, args: Array[String]): Boolean = {
    sender match {
      case player: Player =>
        TagUtil.openTagInventory(player, filter = args(0))
        return true
        if (args.length == 0) {
          new BukkitRunnable {
            override def run(): Unit = {
              val c = new ComponentBuilder("§a: = = 所持しているタグ一覧 §bクリックして変更！ §a= = :\n")
              c.append("-> ").color(ChatColor.WHITE)
                .append("リセットする\n").color(ChatColor.RED)
                .event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tag -reset"))
              db.getTags(player.getUniqueId.toString)
                .map(f => (f, TagUtil.cache.getOrElse(f, "Unknown")))
                .foreach(f => {
                  c.append("-> ").color(ChatColor.WHITE)
                    .append(ChatColor.translateAlternateColorCodes('&', f._2))
                    .event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, s"/tag ${f._1}"))
                    .append("\n")
                })
              player.sendMessage(c.create(): _*)
            }
          }.runTask(WarsCore.instance)
        } else if (args(0).equalsIgnoreCase("-reset")) {
          TagUtil.setTag(player, "")
          player.sendMessage(ChatColor.AQUA + "タグをリセットしました！")
        } else if (args(0).equalsIgnoreCase("-list") && player.hasPermission("pokkedoll.admin")) {
          val comp = new ComponentBuilder("タグ一覧\n").color(ChatColor.GREEN)
          TagUtil.cache.foreach(f => {
            comp.append(s"${f._1} = ${f._2}\n")
          })
          player.sendMessage(comp.append("//").color(ChatColor.GREEN).create(): _*)
        } else if (args(0).equalsIgnoreCase("-reload") && player.hasPermission("pokkedoll.admin")) {
          TagUtil.reloadConfig()
          player.sendMessage("リロードしました")
          // タグを設定する
        } else if (args(0).equalsIgnoreCase("-create") && player.isOp) {
          if(args.length > 1) {
            TagUtil.cache.get(args(1)) match {
              case Some(tag) =>
                val i = new ItemStack(Material.NAME_TAG)
                val m = i.getItemMeta
                m.setDisplayName(ChatColor.WHITE + "タグ: " + tag)
                m.setLore(java.util.Arrays.asList(ChatColor.YELLOW + s"タグ名: " + tag, ChatColor.DARK_PURPLE + "クリックしてタグを入手！"))
                i.setItemMeta(m)
                player.getInventory.addItem(i)
              case None =>
            }

          }
        } else {
          new BukkitRunnable {
            override def run(): Unit = {
              if (TagUtil.hasTag(player, args(0))) {
                TagUtil.setTag(player, args(0))
                player.sendMessage(ChatColor.AQUA + "タグをセットしました!")
              } else {
                player.sendMessage(ChatColor.RED + "タグを所持していないか、無効なタグです")
              }
            }
          }.runTask(WarsCore.instance)
        }
      case _ =>
    }
    true
  }
}
