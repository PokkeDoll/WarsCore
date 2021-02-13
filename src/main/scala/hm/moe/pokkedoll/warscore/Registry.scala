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
    )
  }

  val PAGE_KEY: NamespacedKey = map("PAGE_KEY")

  val WEAPON_TYPE_KEY: NamespacedKey = map("WEAPON_TYPE_KEY")

  val WEAPON_KEY: NamespacedKey = map("WEAPON_KEY")

  val SORT_TYPE_KEY: NamespacedKey = map("SORT_TYPE_KEY")
}
