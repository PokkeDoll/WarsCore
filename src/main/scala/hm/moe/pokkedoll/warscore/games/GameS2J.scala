package hm.moe.pokkedoll.warscore.games

import org.bukkit.entity.Player
import org.jetbrains.annotations.{NotNull, Nullable}

object GameS2J {
  @Nullable
  def getData(@NotNull game: Game, player: Player): GamePlayerData = {
    game match {
      case tdm: TeamDeathMatch => tdm.data.get(player).orNull
      case dom: Domination => dom.data.get(player).orNull
      case _ => null
    }
  }
}
