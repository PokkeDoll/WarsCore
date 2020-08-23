package hm.moe.pokkedoll.warscore

import hm.moe.pokkedoll.warscore.games.Game
import org.bukkit.ChatColor
import org.bukkit.entity.Player

/**
 * WarsCoreのキャッシュを保存するクラス
 */
class WPlayer(val player: Player) {

  var game: Option[Game] = None

  var changeInventory = true

  def sendMessage(string: String): Unit = {
    player.sendMessage(ChatColor.translateAlternateColorCodes('&', string))
  }
}
