package hm.moe.pokkedoll.warscore

import hm.moe.pokkedoll.warscore.games.Game
import net.md_5.bungee.api.chat.BaseComponent
import org.bukkit.ChatColor
import org.bukkit.entity.Player

/**
 * WarsCoreのキャッシュを保存するクラス
 */
class WPlayer(val player: Player) {

  var game: Option[Game] = None

  var changeInventory = true

  /**
   * 現在のランクのキャッシュ
   */
  var rank = 0

  var tag = ""

  def sendMessage(string: String): Unit = {
    player.sendMessage(ChatColor.translateAlternateColorCodes('&', string))
  }

  def sendMessage(components: BaseComponent*): Unit = {
    player.sendMessage(components: _*)
  }
}
