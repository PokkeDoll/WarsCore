package hm.moe.pokkedoll.warscore.wplayer

import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.{Component, TextComponent}

object WPlayerState {
  case object ONLINE extends WPlayerState("online", Component.text("オンライン").color(NamedTextColor.GREEN))
  case object OFFLINE extends WPlayerState("offline", Component.text("オフライン").color(NamedTextColor.GRAY))
  case object ENTRY extends WPlayerState("entry", Component.text("待機中").color(NamedTextColor.GREEN))
  case object PLAYING extends WPlayerState("playing", Component.text("プレイ中").color(NamedTextColor.GREEN))
}

sealed abstract class WPlayerState (
  val name: String,
  val component: TextComponent
)


