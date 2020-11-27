package hm.moe.pokkedoll.warscore.events

import hm.moe.pokkedoll.warscore.games.Game
import org.bukkit.entity.Player
import org.bukkit.event.{Event, HandlerList}

/**
 * 試合で死亡イベントが発生したときに呼び出されるイベント。<br>
 * 正確には試合の死亡処理が行われた後に呼び出される。<br>
 * 注意: 読み込み専用! キャンセルできない<br>
 * 注意: 試合中でのみ呼び出される！ <br>
 * @param attacker キルしたプレイヤー。存在しない場合は **null**
 * @param victim キルされたプレイヤー
 * @param game 「キルされたプレイヤー」が参加している試合
 */
class GameDeathEvent(val game: Game, val attacker: Player, val victim: Player) extends Event {

  def getAttacker: Player = attacker

  def getVictim: Player = victim

  def getGame: Game = game

  override def getHandlers: HandlerList = GameDeathEvent.handlers
}

object GameDeathEvent {
  val handlers = new HandlerList()
  def getHandlerList: HandlerList = handlers
}
