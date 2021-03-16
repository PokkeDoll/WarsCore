package hm.moe.pokkedoll.warscore.events

import hm.moe.pokkedoll.warscore.games.Game
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.event.{Event, HandlerList}

/**
 * プレイヤーがリスポーンしたときに呼ばれるイベント
 *
 * @param game            プレイ中のゲーム
 * @param player          リスポーンしたプレイヤー
 * @param respawnLocation リスポーン予定の座標
 */
class GameRespawnEvent(val game: Game, val player: Player, private var respawnLocation: Location) extends Event {

  def getRespawnLocation: Location = respawnLocation

  def setRespawnLocation(respawnLocation: Location): Unit = this.respawnLocation = respawnLocation

  override def getHandlers: HandlerList = GameRespawnEvent.handlers
}

object GameRespawnEvent {
  val handlers = new HandlerList()

  def getHandlerList: HandlerList = handlers
}
