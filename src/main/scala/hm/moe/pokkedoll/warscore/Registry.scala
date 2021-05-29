package hm.moe.pokkedoll.warscore

import org.bukkit.NamespacedKey

object Registry {

  private var map = Map.empty[String, NamespacedKey]

  protected[warscore] def init(): Unit = {
    val plugin = WarsCore.instance
    map ++= Map(
      "PAGE_KEY" -> new NamespacedKey(plugin, "ui-page"),
      "WEAPON_TYPE_KEY" -> new NamespacedKey(plugin, "weapon-type"),
      "WEAPON_KEY" -> new NamespacedKey(plugin, "weapon-key"),
      "SORT_TYPE_KEY" -> new NamespacedKey(plugin, "sort-type"),
      "GAME_ID" -> new NamespacedKey(plugin, "game-id"),
      "GAME_ISOLATION_ID" -> new NamespacedKey(plugin, "game-isolation-id")
    )
  }

  lazy val PAGE_KEY: NamespacedKey = map("PAGE_KEY")

  lazy val WEAPON_TYPE_KEY: NamespacedKey = map("WEAPON_TYPE_KEY")

  lazy val WEAPON_KEY: NamespacedKey = map("WEAPON_KEY")

  lazy val SORT_TYPE_KEY: NamespacedKey = map("SORT_TYPE_KEY")

  lazy val GAME_ID: NamespacedKey = map("GAME_ID")

  lazy val GAME_ISOLATION_ID: NamespacedKey = map("GAME_ISOLATION_ID")
}
