package hm.moe.pokkedoll.warscore

import java.net.InetSocketAddress
import java.util.UUID

import hm.moe.pokkedoll.warscore.events.PlayerUnfreezeEvent
import hm.moe.pokkedoll.warscore.games.{Game, Tactics, TeamDeathMatch}
import hm.moe.pokkedoll.warscore.utils.{MapInfo, WorldLoader}
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.{Player, Projectile}
import org.bukkit.event.entity.{EntityDamageByEntityEvent, EntityDamageEvent}
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.{Bukkit, ChatColor, Location, Material}
import org.bukkit.scoreboard.{DisplaySlot, Objective, Scoreboard, ScoreboardManager, Team}

import scala.collection.mutable
import scala.util.Random

/**
 * 便利なメソッドをまとめたオブジェクト
 *
 * @author Emorard
 */
object WarsCoreAPI {
  // 内部バージョン. 特に意味はない
  val VERSION = 1

  lazy val scoreboardManager: ScoreboardManager = Bukkit.getScoreboardManager

  lazy val scoreboard: Scoreboard = scoreboardManager.getNewScoreboard

  lazy val random = new Random()

  protected[warscore] var DEFAULT_SPAWN: Location = _

  /** ゲーム情報 */
  val games = mutable.HashMap.empty[String, Game]

  /** プレイヤーのキャッシュ */
  val wplayers = mutable.HashMap.empty[Player, WPlayer]

  /** ワールドの設定 */
  @Deprecated
  var worldSettingConfig: ConfigurationSection = _

  /** マップ情報 */
  var mapinfo = Seq.empty[MapInfo]

  /**
   * リソースパック情報
   */
  var rsInfo = mutable.HashMap.empty[String, String]

  /**
   * スコアボードたち
   */
  val scoreboards = mutable.HashMap.empty[Player, Scoreboard]

  /**
   * チームの設定をまとめたもの
   * @param team
   */
  def setBaseTeam(team: Team): Unit = {
    team.setAllowFriendlyFire(false)
    team.setCanSeeFriendlyInvisibles(true)
    team.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER)
    team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.FOR_OWN_TEAM)
  }

  /**
   * テストコード
   * @param player
   * @return
   */
  def getWPlayer(player: Player): WPlayer = wplayers.getOrElseUpdate(player, new WPlayer(player))

  /**
   * プレイヤーの動きを止める。視点は動かせる
   * @param player
   */
  def freeze(player: Player): Unit = {
    player.teleport(player.getLocation().add(0, 0.001, 0))
    player.setAllowFlight(true)
    player.setFlying(true)
    player.setWalkSpeed(0.001f)
    player.setFlySpeed(0.001f)
  }

  /**
   * プレイヤーを動けるようにする。freezeと共に使用する
   * @see freeze(Player)
   */
  def unfreeze(player: Player): Unit = {
    val event = new PlayerUnfreezeEvent(player)
    Bukkit.getServer.getPluginManager.callEvent(event)
    player.setAllowFlight(false)
    player.setFlying(false)
    player.setWalkSpeed(event.walkSpeed)
    player.setFlySpeed(event.flySpeed)
  }

  /**
   * 説明しよう！(図で)<br>
   * mapinfo:             <- ↑ cs ↑<br>
   *     tdm:             <- gameType<br>
   *         mapA:        <- id<br>
   *             author:<br>
   *             spawn:<br>
   *             ...<br>
   * @param cs
   */
  def reloadMapInfo(cs: ConfigurationSection): Unit = {
    cs.getKeys(false).forEach(gameType => {
      mapinfo = Seq.empty[MapInfo]

      cs.getConfigurationSection(gameType).getKeys(false).forEach(id => {
        val i = new MapInfo(gameType, id)
        i.mapName = cs.getString(s"$gameType.$id.mapName")
        i.authors = cs.getString(s"$gameType.$id.authors")
        cs.getConfigurationSection(s"$gameType.$id.location").getKeys(false).forEach(location => {
          val str = cs.getString(s"$gameType.$id.location.$location")
          val data = str.split(",")
          try {
            i.locations.put(location, (data(0).toDouble, data(1).toDouble, data(2).toDouble, data(3).toFloat, data(4).toFloat))
          } catch {
            case e: ArrayIndexOutOfBoundsException =>
              e.printStackTrace()
            case e: NumberFormatException =>
              e.printStackTrace()
          }
        })
        mapinfo = mapinfo :+ i
      })
    })
  }

  /**
   * ゲームの情報を読み込む
   * @param cs
   */
  def reloadGame(cs: ConfigurationSection): Unit = {
    games.clear()

    games.put("tdm-test-1", new TeamDeathMatch("tdm-test-1"))
    games.put("tdm-test-2", new TeamDeathMatch("tdm-test-2"))

    games.put("tactics-test-1", new Tactics("tactics-test-1"))

    if(Bukkit.getWorld("tdm-test-1") != null) WorldLoader.syncUnloadWorld("tdm-test-1")
    if(Bukkit.getWorld("tdm-test-2") != null) WorldLoader.syncUnloadWorld("tdm-test-2")

    if(Bukkit.getWorld("tactics-test-1") != null) WorldLoader.syncUnloadWorld("tactics-test-1")
  }

  /**
   * リソースパックの情報を読み込む
   */
  def reloadRs(cs: ConfigurationSection): Unit = {
    rsInfo.clear()
    cs.getKeys(false).forEach(key => {
      rsInfo.put(key, cs.getString(key, ""))
    })
  }

  /**
   * プレイヤーの所持している武器名を取得する
   * @param player
   * @return
   */
  def getAttackerWeaponName(player: Player): Option[String] = {
    val item = player.getInventory.getItemInMainHand
    if (item == null) None
    else {
      val meta = item.getItemMeta
      if (meta.hasDisplayName) Some(meta.getDisplayName) else Some(item.getType.toString)
    }
  }

  val GAME_INVENTORY_TITLE = "§aゲーム一覧！"

  /**
   * ゲーム情報GUI版<br>
   * 重み:<br>
   * tdm: 0<br>
   * tactics: 1(暫定)
   * @param player
   */
  def showGameInventory(player: Player): Unit = {
    val inv = Bukkit.createInventory(null, 54, GAME_INVENTORY_TITLE)
    var slot = (-1, 8, 17)
    games.foreach(f => {
      val weight = if(f._1.startsWith("tdm")) {
        slot = (slot._1 + 1, slot._2, slot._3)
        slot._1
      } else if (f._1.startsWith("tactics")) {
        slot = (slot._1, slot._2 + 1, slot._3)
        slot._2
      } else slot._3
      val icon = ((damage => new ItemStack(Material.INK_SACK, f._2.members.size + 1, damage)): Short => ItemStack)(if(f._2.state.join) 10 else 8)
      val meta = icon.getItemMeta
      meta.setDisplayName((if(f._1.contains("test")) ChatColor.RED else ChatColor.WHITE) + s"${f._2.title}")
      meta.setLore(java.util.Arrays.asList(s"§e${f._2.description}", s"§a${f._2.members.size} §7/ §a${f._2.maxMember} プレイ中", s"§a${f._2.state.title}", "//TODO ほかにも追加"))
      icon.setItemMeta(meta)
      inv.setItem(weight, icon)
    })
    player.openInventory(inv)
  }

  def addScoreBoard(player: Player): Unit = {
    val board = scoreboardManager.getNewScoreboard
    val obj = board.registerNewObjective("status", "dummy")

    obj.setDisplayName("ハローユーチューブ")
    obj.setDisplaySlot(DisplaySlot.SIDEBAR)

    val rank = obj.getScore("§9Rank: -")
    rank.setScore(5)

    val exp = obj.getScore("§9EXP: -1 / -1")
    exp.setScore(4)

    val etc = obj.getScore("§6etc.")
    etc.setScore(3)

    // 名前の下に書くやつ
    val tag = board.registerNewObjective("tag", "dummy")
    tag.setDisplayName(ChatColor.translateAlternateColorCodes('&', s"&a(ここにTAG) &f0"))
    tag.getScore(player.getName).setScore(0)
    tag.setDisplaySlot(DisplaySlot.BELOW_NAME)

    player.setScoreboard(board)

    /* 自分 */
    val team = board.registerNewTeam(player.getName)
    team.setPrefix(ChatColor.translateAlternateColorCodes('&', "&7[&a000&7]&r "))
    team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS)
    team.addEntry(player.getName)

    scoreboards.foreach(f => {
      WarsCore.instance.getLogger.info(s"WarsCoreAPI.addScoreboard(${player.getName})")
      /* タグの問題 */
      val oTag = f._2.getObjective("tag")
      if(oTag!=null)
        oTag.getScore(player.getName).setScore(0)
      else
        WarsCore.instance.getLogger.info(s"oTag is null! ${f._2}")
      /* 他プレイヤーに対して */
      val oTeam = f._2.registerNewTeam(player.getName)
      oTeam.setPrefix(ChatColor.translateAlternateColorCodes('&', "&7[&a000&7]&r "))
      oTeam.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS)
      oTeam.addEntry(player.getName)

      /* 自分に対して */
      // tag.getScore(f._1.getName).setScore(f._2.getObjective("tag").getScore(f._1.getName).getScore)
      tag.getScore(f._1.getName).setScore(0)

      val mTeam = board.registerNewTeam(f._1.getName)
      mTeam.setPrefix(ChatColor.translateAlternateColorCodes('&', "&7[&a000&7]&r "))
      mTeam.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS)
      mTeam.addEntry(f._1.getName)
    })

    scoreboards.put(player, board)
  }

  def removeScoreboard(player: Player): Unit = {
    scoreboards.remove(player)
    scoreboards.values.foreach(b => {
      Option(b.getTeam(player.getName)).foreach(_.unregister())
      b.resetScores(player.getName)
    })
  }
}
