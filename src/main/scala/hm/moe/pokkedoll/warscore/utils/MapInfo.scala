package hm.moe.pokkedoll.warscore.utils

import org.jetbrains.annotations.NotNull

/**
 * Q: なぜLoaction型じゃないんですか！？<br>
 * A: Game.load()するたびにワールドの情報が変わるため、読み込みエラーが発生してしまう。 <br>
 *
 * @note 単体では使わない。WarsCoreAPIで使う。
 * @see WarsCoreAPI#reloadMapinfo
 * @constructor ゲームの識別IDとマップIDを作成
 * @param mapId マップのID, yamaとかtaniとか
 * @author Emorard
 * @version 2
 */
//TODO MapInfo.ofの追加
class MapInfo(@NotNull val mapId: String = "Unknown",
              @NotNull val mapName: String = "Unknown",
              @NotNull val authors: String = "Unknown",
              @NotNull val locations: Map[String, WeakLocation] = Map.empty[String, WeakLocation],
              @NotNull val metadata: Map[String, Any] = Map.empty[String, Any])