package hm.moe.pokkedoll.warscore

import hm.moe.pokkedoll.warscore.games.Game
import net.md_5.bungee.api.chat.BaseComponent
import org.bukkit.ChatColor
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

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

  var _tag = ""
  @Deprecated
  var tag = ""

  def sendMessage(string: String): Unit = {
    player.sendMessage(ChatColor.translateAlternateColorCodes('&', string))
  }

  def sendMessage(components: BaseComponent*): Unit = {
    player.sendMessage(components: _*)
  }

  var virtualPlayerInventory: Array[ItemStack] = new Array[ItemStack](36)

  var virtualGameInventory: Array[ItemStack] = new Array[ItemStack](36)
}
