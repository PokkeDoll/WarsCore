package hm.moe.pokkedoll.warscore

import hm.moe.pokkedoll.warscore.games.Game
import hm.moe.pokkedoll.warscore.utils.VirtualInventory
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

  /**
   * 現在の経験値のキャッシュ
   */
  var exp = 0

  var tag = ""

  def sendMessage(string: String): Unit = {
    player.sendMessage(ChatColor.translateAlternateColorCodes('&', string))
  }

  def sendMessage(components: BaseComponent*): Unit = {
    player.sendMessage(components: _*)
  }

  var virtualNormalInventory: Option[VirtualInventory] = None

  var virtualGameInventory: Option[VirtualInventory] = None
}
