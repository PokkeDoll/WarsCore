package hm.moe.pokkedoll.warscore

import java.awt.ItemSelectable

import hm.moe.pokkedoll.warscore.games.Game
import hm.moe.pokkedoll.warscore.utils.VirtualInventory
import net.md_5.bungee.api.chat.BaseComponent
import org.bukkit.ChatColor
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

/**
 * WarsCoreのキャッシュを保存するクラス
 */
class WPlayer(val player: Player) {

  var game: Option[Game] = None

  /**
   * ゲームインスタンスを取得する
   * @return ゲームに参加していないならnull。必ずチェックすること！
   */
  def getGameAsJava: Game = game.orNull

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

  var isShowTag = false

  def sendMessage(string: String): Unit = {
    player.sendMessage(ChatColor.translateAlternateColorCodes('&', string))
  }

  def sendMessage(components: BaseComponent*): Unit = {
    player.sendMessage(components: _*)
  }

  var disconnect: Boolean = false
}
