package hm.moe.pokkedoll.warscore.utils

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
class MapInfo(val mapId: String = "Unknown", val mapName: String = "Unknown", val authors: String = "Unknown", val locations: Map[String, WeakLocation] = Map.empty[String, WeakLocation])
