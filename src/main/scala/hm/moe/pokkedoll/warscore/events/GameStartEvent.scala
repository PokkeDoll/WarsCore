package hm.moe.pokkedoll.warscore.events

import hm.moe.pokkedoll.warscore.games.Game
import org.bukkit.event.{Event, HandlerList}

/**
 * 試合が始まった時に呼び出されるイベント <br>
 * 正確にはチーム分けがされ、最初のスポーンに移動した後に呼び出される。 <br>
 * 注意: 読み取り専用！キャンセルできない。<br>
 *
 * @param game そのもの
 */
class GameStartEvent(private val game: Game) extends Event {

  def getGame: Game = game

  override def getHandlers: HandlerList = GameStartEvent.handlers
}

object GameStartEvent {
  val handlers = new HandlerList()
  def getHandlerList: HandlerList = handlers
}
