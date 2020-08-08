package hm.moe.pokkedoll.warscore

import hm.moe.pokkedoll.warscore.games.Game
import org.bukkit.entity.Player

/**
 * WarsCoreのキャッシュを保存するクラス
 */
class WPlayer(val player: Player) {

  var game: Option[Game] = None

  var changeInventory = true

  /**
   * TDMの情報ッッッッ！
   */
  var play, win, kill, death, assist, damage: Int = _

  /**
   * タグ情報
   */
  var showTag: Boolean = false

  var currentTag: String = _

  var storageTag: Set[String] = _

  def sendMessage(string: String): Unit = {
    player.sendMessage(string: String)
  }
}
