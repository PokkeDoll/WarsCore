package hm.moe.pokkedoll.warscore

import hm.moe.pokkedoll.warscore.events.PlayerUnfreezeEvent
import hm.moe.pokkedoll.warscore.games.{Domination, Game, Tactics, TeamDeathMatch}
import hm.moe.pokkedoll.warscore.ui.WeaponUI.{EMPTY, ITEM, MAIN, MELEE, SUB}
import hm.moe.pokkedoll.warscore.utils.{MapInfo, RankManager, TagUtil, WorldLoader}
import net.md_5.bungee.api.chat.{BaseComponent, ClickEvent, ComponentBuilder, HoverEvent}
import org.bukkit._
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.{EntityType, Firework, Player}
import org.bukkit.inventory.{ItemFlag, ItemStack}
import org.bukkit.persistence.PersistentDataType
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.scoreboard.{DisplaySlot, Scoreboard, ScoreboardManager, Team}

import scala.collection.mutable
import scala.util.Random

/**
 * 便利なメソッドをまとめたオブジェクト
 *
 * @author Emorard
 */
object WarsCoreAPI {
  // 内部バージョン. 特に意味はない
  @Deprecated
  val VERSION = 1

  val LOBBY = "p-lobby"

  lazy val scoreboardManager: ScoreboardManager = Bukkit.getScoreboardManager

  lazy val scoreboard: Scoreboard = scoreboardManager.getNewScoreboard

  lazy val random = new Random()

  protected[warscore] var DEFAULT_SPAWN: Location = _

  protected[warscore] var FIRST_SPAWN: Location = _

  /** ゲーム情報 */
  val games = mutable.HashMap.empty[String, Game]

  /** プレイヤーのキャッシュ */
  val wplayers = new mutable.HashMap[Player, WPlayer](50, 1.0)

  /** マップ情報 */
  var mapinfo = Seq.empty[MapInfo]

  /**
   * スコアボードたち
   */
  val scoreboards = mutable.HashMap.empty[Player, Scoreboard]

  /**
   * チームの設定をまとめたもの
   *
   * @param team Team
   */
  def setBaseTeam(team: Team): Unit = {
    team.setAllowFriendlyFire(false)
    team.setCanSeeFriendlyInvisibles(true)
    team.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER)
    team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.FOR_OTHER_TEAMS)
  }

  /**
   * APIにキャッシュされているインスタンスを返す。ないなら作る
   *
   * @param player Player
   * @return
   */
  def getWPlayer(player: Player): WPlayer = {
    wplayers.get(player) match {
      case Some(wp) =>
        wp
      case None =>
        val wp = new WPlayer(player)
        wplayers.put(player, wp)
        database.loadWPlayer(wp, new Callback[WPlayer] {
          override def success(value: WPlayer): Unit = {
            addScoreBoard(player)
            println(value.disconnect)
            if(value.disconnect) {
              value.disconnect = false
              database.setDisconnect(player.getUniqueId.toString, disconnect = false)
              WarsCoreAPI.restoreLobbyInventory(player)
              player.teleport(WarsCoreAPI.DEFAULT_SPAWN)
            }
          }

          override def failure(error: Exception): Unit = {
            error.printStackTrace()
            player.sendMessage(ChatColor.RED + "データの読み込みに失敗しました")
          }
        })
        wp
    }
  }

  /**
   * プレイヤーの動きを止める。視点は動かせる
   *
   * @param player Player
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
   *
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
   * tdm:             <- gameType<br>
   * mapA:        <- id<br>
   * author:<br>
   * spawn:<br>
   * ...<br>
   *
   * @param cs mapinfo
   */
  def reloadMapInfo(cs: ConfigurationSection): Unit = {
    mapinfo = Seq.empty[MapInfo]
    cs.getKeys(false).forEach(gameType => {
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
   *
   * @param cs game
   */
  def reloadGame(cs: ConfigurationSection): Unit = {
    games.clear()

    (1 to 4) foreach (id => {
      games.put(s"tdm-$id", new TeamDeathMatch(s"tdm-$id"))
      if (Bukkit.getWorld(s"tdm-$id") != null) WorldLoader.syncUnloadWorld(s"tdm-$id")
    })

    (1 to 2) foreach (id => {
      games.put(s"dom-$id", new Domination(s"dom-$id"))
      if (Bukkit.getWorld(s"dom-$id") != null) WorldLoader.syncUnloadWorld(s"dom-$id")
    })

    games.put("tactics-1", new Tactics("tactics-1"))
    if (Bukkit.getWorld("tactics-1") != null) WorldLoader.syncUnloadWorld("tactics-1")
  }

  /**
   * プレイヤーの所持している武器名を取得する
   *
   * @param player Player
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

  private val database = WarsCore.instance.database

  /**
   * スコアボードを更新する
   * スコアボードはscoreboardsにすでに存在するものとする
   *
   * @param player Player
   */
  @Deprecated
  def updateScoreboard(player: Player, scoreboard: Scoreboard): Unit = {
    val test = new Test("updateScoreboard")
    new BukkitRunnable {
      override def run(): Unit = {
        val uuid = player.getUniqueId.toString
        val wp = WarsCoreAPI.wplayers(player)
        // ランクを取得する
        val rankData = (wp.rank, wp.exp)
        val tagData = TagUtil.cache.getOrElse(wp.tag, "-")

        wp.rank = rankData._1
        RankManager.updateSidebar(scoreboard, data = rankData)

        // タグ
        val tag = Option(scoreboard.getObjective("tag")) match {
          case Some(tag) =>
            //println("tag is some!")
            tag.setDisplayName(ChatColor.translateAlternateColorCodes('&', s"$tagData &f0"))
            tag
          case None =>
            //println("tag is none!")
            val tag = scoreboard.registerNewObjective("tag", "dummy")
            tag.setDisplayName(ChatColor.translateAlternateColorCodes('&', s"$tagData &f0"))
            tag.getScore(player.getName).setScore(0)
            tag.setDisplaySlot(DisplaySlot.BELOW_NAME)
            tag
        }

        // ランク
        Option(scoreboard.getTeam(player.getName)) match {
          case Some(rank) =>
            rank.setPrefix(ChatColor.translateAlternateColorCodes('&', s"&7[&a${rankData._1}&7]&r "))
            player.setPlayerListName(ChatColor.translateAlternateColorCodes('&', s"&7[&a${rankData._1}&7]&r ${player.getName}"))
          case None =>
            val rank = scoreboard.registerNewTeam(player.getName)
            rank.setPrefix(ChatColor.translateAlternateColorCodes('&', s"&7[&a${rankData._1}&7]&r "))
            rank.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS)
            rank.addEntry(player.getName)
        }

        player.setScoreboard(scoreboard)

        // スコアボードをリフレッシュする
        scoreboards.filterNot(_._1 == player).foreach(f => {
          {
            WarsCore.instance.getLogger.info(s"WarsCoreAPI.addScoreboard(${player.getName})")
            /* タグの問題 */
            val oTag = f._2.getObjective("tag")
            if (oTag != null)
              oTag.getScore(player.getName).setScore(0)
            else
              WarsCore.instance.getLogger.info(s"oTag is null! ${f._2}")
            /* 他プレイヤーに対して */
            //val oTeam = f._2.registerNewTeam(player.getName)
            val oTeam = Option(f._2.getTeam(player.getName)).getOrElse(f._2.registerNewTeam(player.getName))
            oTeam.setPrefix(ChatColor.translateAlternateColorCodes('&', s"&7[&a${rankData._1}&7]&r "))
            oTeam.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS)
            oTeam.addEntry(player.getName)

            /* 自分に対して */
            tag.getScore(f._1.getName).setScore(0)

            val mTeam = Option(scoreboard.getTeam(f._1.getName)).getOrElse(scoreboard.registerNewTeam(f._1.getName))
            mTeam.setPrefix(ChatColor.translateAlternateColorCodes('&', s"&7[&a${rankData._1}&7]&r "))
            mTeam.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS)
            mTeam.addEntry(f._1.getName)
          }
        })
      }
    }.runTask(WarsCore.instance)
    test.log(20L)
  }

  def addScoreBoard(player: Player): Unit = {
    updateScoreboard(player, scoreboards.getOrElseUpdate(player, scoreboardManager.getNewScoreboard))
  }

  def removeScoreboard(player: Player): Unit = {
    scoreboards.remove(player)
    scoreboards.values.foreach(b => {
      Option(b.getTeam(player.getName)).foreach(_.unregister())
      b.resetScores(player.getName)
    })
  }

  def randomChance(chance: Double): Boolean = (chance / 100.0) > Math.random()

  def spawnFirework(location: Location): Unit = {
    val firework: Firework = location.getWorld.spawnEntity(location, EntityType.FIREWORK).asInstanceOf[Firework]
    val meta = firework.getFireworkMeta
    val effect: FireworkEffect = FireworkEffect.builder().`with`(FireworkEffect.Type.BALL).withColor(Color.YELLOW).build()
    meta.addEffect(effect)
    firework.setFireworkMeta(meta)
  }

  import net.md_5.bungee.api.ChatColor

  val NEWS: Array[BaseComponent] =
    new ComponentBuilder("= = = = = = = = = = =").color(ChatColor.GREEN).underlined(true)
      .append("お知らせ").underlined(false)
      .append("= = = = = = = = = = =\n\n").underlined(true)
      .append("* ").reset().append("Discordに参加しよう！ こちらのメッセージをクリックしてください！\n").color(ChatColor.AQUA).event(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://discordapp.com/invite/TJ3bkkY"))
      // .append("* ").reset().append("不具合情報/開発状況はマイルストーンにまとめています！\n").event(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://gitlab.com/PokkeDoll/pokkedoll/-/milestones/1"))
      .append("* ").reset().append("βテスト開催中！  /game よりゲームに参加！")
      .create()


  def splitToComponentTimes(biggy: BigDecimal): (Int, Int, Int) = {
    val long = biggy.longValue
    val hours = (long / 3600).toInt
    var remainder = (long - hours * 3600).toInt
    val mins = remainder / 60
    remainder = remainder - mins * 60
    val secs = remainder
    (hours, mins, secs)
  }

  def getLocation(string: String): Option[Location] = {
    val arr = string.split(",")
    try {
      val location = new Location(Bukkit.getWorld(arr(0)), arr(1).toDouble, arr(2).toDouble, arr(3).toDouble, arr(4).toFloat, arr(5).toFloat)
      Some(location)
    } catch {
      case e: Exception =>
        e.printStackTrace()
        None
    }
  }

  def noticeStartGame(game: Game): Unit = {
    val gameInfo = new ComponentBuilder(game.title + "\n").bold(true).underlined(true)
      .append(game.description + "\n").color(ChatColor.YELLOW).italic(true).bold(false).underlined(false)
      .append(s"${game.members.size} / ${game.maxMember} プレイ中\n").color(ChatColor.GREEN).italic(false)
    val comp = new ComponentBuilder("[お知らせ] ").color(ChatColor.AQUA)
      .append(game.title).color(ChatColor.GREEN)
      .append(s"が始まりました！ ").color(ChatColor.AQUA)
      .append(s"/game join ${game.id}").color(ChatColor.LIGHT_PURPLE)
      .append("または、このメッセージをクリックしてください！").color(ChatColor.AQUA)
      .event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, s"/game join ${game.id}"))
      .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, gameInfo.create()))
      .create()
    //Bukkit.getWorlds.get(0).getPlayers.forEach(_.sendMessage(comp:_*))
    //
    //Bukkit.broadcast(new TextComponent("テスト用メッセ"))
    Bukkit.broadcast(comp: _*)
  }

  def playBattleSound(game: Game): Unit = {
    val n = random.nextInt(3) + 1
    game.members.map(_.player).foreach(p => p.playSound(p.getLocation, s"battle$n", 1f, 1f))
  }

  def createFirework(location: Location, color: Color, `type`: FireworkEffect.Type): Unit = {
    val fw = location.getWorld.spawnEntity(location, EntityType.FIREWORK).asInstanceOf[Firework]
    val meta = fw.getFireworkMeta
    meta.addEffect(FireworkEffect.builder().withColor(color).`with`(`type`).build())
    fw.setFireworkMeta(meta)
  }

  val LEVEL_INFO = "INFO"
  val LEVEL_WARN = "WARN"
  val LEVEL_ERROR = "ERROR"
  val LEVEL_DEBUG = "DEBUG"

  def gameLog(gameid: String, level: String, message: String): Unit = {
    database.gameLog(gameid, level, message)
  }

  object UI {
    val PAGE_KEY = new NamespacedKey(WarsCore.instance, "ui-page")

    val PAGE_ICON: Int => ItemStack = (page: Int) => {
      val i = new ItemStack(Material.WRITABLE_BOOK)
      val m = i.getItemMeta
      m.setDisplayName(ChatColor.translateAlternateColorCodes('&', s"&e${if (page == 1) "-" else page - 1} &7← &a&l$page &r&7→ &e${page + 1}"))
      m.setLore(java.util.Arrays.asList("左クリック | - | 右クリック"))
      m.getPersistentDataContainer.set(PAGE_KEY, PersistentDataType.INTEGER, java.lang.Integer.valueOf(page))
      i.setItemMeta(m)
      i
    }

    val PANEL: ItemStack = {
      val i = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE)
      val m = i.getItemMeta
      m.setDisplayName(" ")
      i.setItemMeta(m)
      i
    }
  }

  /**
   * データベースで指定されているアイテムを取得する
   *
   * @param wp     対象のプレイヤー
   * @param cached WPlayerに保存されているキャッシュを利用する
   */
  def loadWeaponInventory(wp: WPlayer, cached: Boolean = true): Unit = {
    val player = wp.player
    wp.weapons match {
      case Some(weapon) if cached =>
        player.getInventory.setContents(weapon)
      case _ =>
        database.getWeapon(wp.player.getUniqueId.toString, new Callback[mutable.Buffer[(Array[Byte], Int)]] {
          override def success(value: mutable.Buffer[(Array[Byte], Int)]): Unit = {

            val main = value.find(p => p._2 == 1) match {
              case Some(f) => ItemStack.deserializeBytes(f._1)
              case None => EMPTY
            }
            val sub = value.find(p => p._2 == 2) match {
              case Some(f) => ItemStack.deserializeBytes(f._1)
              case None => EMPTY
            }
            val melee = value.find(p => p._2 == 3) match {
              case Some(f) => ItemStack.deserializeBytes(f._1)
              case None => EMPTY
            }
            val item = value.find(p => p._2 == 4) match {
              case Some(f) => ItemStack.deserializeBytes(f._1)
              case None => EMPTY
            }
            val array = Array(main, sub, melee, item)

            player.getInventory.setContents(array)
            wp.weapons = Some(array)
          }

          override def failure(error: Exception): Unit = {
            player.sendMessage("エラー！")
          }
        })
    }
  }

  /**
   * ロビーのインベントリを退避する
   * @version v1.3.16
   */
  def changeWeaponInventory(wp: WPlayer): Unit = {
    val player = wp.player
    database.setVInv(player.getUniqueId.toString, player.getInventory.getStorageContents, new Callback[Unit] {
      override def success(value: Unit): Unit = {
        loadWeaponInventory(wp)
      }

      override def failure(error: Exception): Unit = {
        wp.sendMessage("ロビーインベントリの読み込みに失敗しました")
      }
    })
  }

  /**
   *
   * @version v1.3.16
   * @param player
   */
  def restoreLobbyInventory(player: Player): Unit = {
    database.getVInv(player.getUniqueId.toString, new Callback[mutable.Buffer[(Int, Array[Byte])]] {
      override def success(value: mutable.Buffer[(Int, Array[Byte])]): Unit = {
        val contents = Array.fill(36)(new ItemStack(Material.AIR))
        value.foreach(f => { contents(f._1) = ItemStack.deserializeBytes(f._2) })
        player.getInventory.setStorageContents(contents)
      }

      override def failure(error: Exception): Unit = {
        player.sendMessage("なんと復元できませんでした")
      }
    })
  }
}
