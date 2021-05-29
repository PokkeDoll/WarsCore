package hm.moe.pokkedoll.warscore.events

import hm.moe.pokkedoll.warscore.WarsCore
import org.bukkit.event.{Event, HandlerList}

/**
 * WarsCoreが読み込まれた時に呼ばれるイベント
 *
 * @param plugin WarsCoreのインスタンス
 */
class InitializeWarsCoreEvent(val plugin: WarsCore) extends Event {
  override def getHandlers: HandlerList = InitializeWarsCoreEvent.handlers
}

object InitializeWarsCoreEvent {
  val handlers = new HandlerList()

  def getHandlerList: HandlerList = handlers
}