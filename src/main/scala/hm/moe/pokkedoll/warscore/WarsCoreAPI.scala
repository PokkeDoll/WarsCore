package hm.moe.pokkedoll.warscore

import hm.moe.pokkedoll.warscore.events.PlayerUnfreezeEvent
import hm.moe.pokkedoll.warscore.games.{Game, Tactics, TeamDeathMatch}
import hm.moe.pokkedoll.warscore.utils.{MapInfo, RankManager, TagUtil, WorldLoader}
import net.md_5.bungee.api.chat.{BaseComponent, ClickEvent, ComponentBuilder, HoverEvent, TextComponent}
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.{EntityType, Firework, Player}
import org.bukkit.inventory.{ItemFlag, ItemStack}
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.scoreboard.{DisplaySlot, Scoreboard, ScoreboardManager, Team}
import org.bukkit._

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

  val LOBBY = "p-lobby"

  lazy val scoreboardManager: ScoreboardManager = Bukkit.getScoreboardManager

  lazy val scoreboard: Scoreboard = scoreboardManager.getNewScoreboard

  lazy val random = new Random()

  protected [warscore] var DEFAULT_SPAWN: Location = _

  protected [warscore] var FIRST_SPAWN: Location = _

  /** ゲーム情報 */
  val games = mutable.HashMap.empty[String, Game]

  /** プレイヤーのキャッシュ */
  val wplayers = new mutable.HashMap[Player, WPlayer](50, 1.0)

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
    team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.FOR_OTHER_TEAMS)
  }

  /**
   * APIにキャッシュされているインスタンスを返す。ないなら作る
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

    (1 to 8) foreach (id => {
      games.put(s"tdm-test-$id", new TeamDeathMatch(s"tdm-test-$id"))
      if(Bukkit.getWorld(s"tdm-test-$id") != null) WorldLoader.syncUnloadWorld(s"tdm-test-$id")
    })

    (1 to 4) foreach (id => {
      games.put(s"tactics-test-$id", new Tactics(s"tactics-test-$id"))
      if(Bukkit.getWorld(s"tactics-test-$id") != null) WorldLoader.syncUnloadWorld(s"tactics-test-$id")
    })
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
  def openGameInventory(player: Player): Unit = {
    val inv = Bukkit.createInventory(null, 18, GAME_INVENTORY_TITLE)
    var slot = (0, 9, 18)
    inv.setItem(0, new ItemStack(Material.BOW))
    inv.setItem(9, new ItemStack(Material.IRON_SWORD))
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
      meta.setDisplayName((if(f._1.contains("test")) ChatColor.RED else ChatColor.WHITE) + s"${f._1}")
      meta.setLore(java.util.Arrays.asList(s"§f${f._2.title}", s"§e${f._2.description}", s"§a${f._2.members.size} §7/ §a${f._2.maxMember} プレイ中", s"§a${f._2.state.title}"))
      icon.setItemMeta(meta)
      inv.setItem(weight, icon)
    })
    player.openInventory(inv)
  }

  private val database = WarsCore.instance.database

  /**
   * スコアボードを更新する
   * スコアボードはscoreboardsにすでに存在するものとする
   * @param player
   */
  def updateScoreboard(player: Player, scoreboard: Scoreboard): Unit = {
    val test = new Test("updateScoreboard")
    new BukkitRunnable {
      override def run(): Unit = {
        val uuid = player.getUniqueId.toString
        val wp = WarsCoreAPI.wplayers(player)
        // ランクを取得する
        val rankData = database.getRankData(uuid).getOrElse((-1, -1))
        val tagData = TagUtil.cache.getOrElse(database.getTag(uuid), "-")

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
            if(oTag!=null)
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


  @Deprecated
  def addScoreBoard(player: Player): Unit = {
    updateScoreboard(player, scoreboards.getOrElseUpdate(player, scoreboardManager.getNewScoreboard))
    /*
    val test = new Test()
    val board = scoreboardManager.getNewScoreboard

    WarsCore.instance.database.getRankData(player.getUniqueId.toString) match {
      case Some(data) =>

        // ついでに更新
        WarsCoreAPI.wplayers(player).rank = data._1
        RankManager.updateSidebar(board, data)

        // 名前の下に書くやつ
        val tag = board.registerNewObjective("tag", "dummy")
        tag.setDisplayName(ChatColor.translateAlternateColorCodes('&', s"&a(ここにTAG) &f0"))
        tag.getScore(player.getName).setScore(0)
        tag.setDisplaySlot(DisplaySlot.BELOW_NAME)

        player.setScoreboard(board)

        /* 自分 */
        val team = board.registerNewTeam(player.getName)
        team.setPrefix(ChatColor.translateAlternateColorCodes('&', s"&7[&a${data._1}&7]&r "))
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
          oTeam.setPrefix(ChatColor.translateAlternateColorCodes('&', s"&7[&a${data._1}&7]&r "))
          oTeam.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS)
          oTeam.addEntry(player.getName)

          /* 自分に対して */
          tag.getScore(f._1.getName).setScore(0)

          val mTeam = board.registerNewTeam(f._1.getName)
          mTeam.setPrefix(ChatColor.translateAlternateColorCodes('&', s"&7[&a${data._1}&7]&r "))
          mTeam.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS)
          mTeam.addEntry(f._1.getName)
        })
      case None =>
    }

    scoreboards.put(player, board)

    test.log("WarsCoreAPI.addScoreboard()")
     */
  }


  def removeScoreboard(player: Player): Unit = {
    scoreboards.remove(player)
    scoreboards.values.foreach(b => {
      Option(b.getTeam(player.getName)).foreach(_.unregister())
      b.resetScores(player.getName)
    })
  }

  /**
   * 統計
   * @param player
   * @param target
   */
  def openStatsInventory(player: Player, target: Player): Unit = {
    val inv = Bukkit.createInventory(null, 18, s"${target.getName}'s stats'")
    val wp = getWPlayer(target)
    val tdmIcon = new ItemStack(Material.IRON_SWORD)
    val tdmIconMeta = tdmIcon.getItemMeta
    tdmIconMeta.setDisplayName(ChatColor.YELLOW + "TDM")
    tdmIconMeta.setLore(java.util.Arrays.asList(
      ChatColor.GRAY + "プレイした記録: " + -1,
      ChatColor.GRAY + "勝利した回数: " + -1,
      ChatColor.GRAY + "敵を倒した回数: " + -1,
      ChatColor.GRAY + "死んだ回数: " + -1,
      ChatColor.GRAY + "与えたダメージ: " + -1,
      ChatColor.GRAY + "*",
      ChatColor.GRAY + "占領に成功した回数: " + -1,
      ChatColor.GRAY + "Kill MVPを取得した回数: " + -1,
      ChatColor.GRAY + "Damage MVPを取得した回数: " * -1
    ))
    tdmIconMeta.setUnbreakable(true)
    tdmIconMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE)
    tdmIcon.setItemMeta(tdmIconMeta)

    val tacticsIcon = new ItemStack(Material.REDSTONE, 1)
    val tacticsIconMeta = tacticsIcon.getItemMeta
    tacticsIconMeta.setDisplayName(ChatColor.DARK_AQUA + "Tactics")
    tacticsIconMeta.setLore(java.util.Arrays.asList(
      "",
      "",
      ""
    ))
  }

  def setChangeInventory(wp: WPlayer): Unit = {
    wp.changeInventory = true
    new BukkitRunnable {
      override def run(): Unit = {
        wp.changeInventory = false
      }
    }.runTaskLaterAsynchronously(WarsCore.instance, 100L)
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
      .append("= = = = = = = = = = =\n").underlined(true)
      .append("*").color(ChatColor.WHITE).underlined(false)
      //.append("開発進捗や計画はすべてGitLabで公開されています\n").color(ChatColor.AQUA).event(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://gitlab.com/PokkeDoll/pokkedoll-mc/-/boards"))
      //.append("*").color(ChatColor.WHITE)
      //.append("バージョン判定されてるけどまだ1.12.2のリソースパックを送信しています\n")
      //.append("*")
      .append("Discordに参加しよう！ こちらメッセージをクリックしてください！").color(ChatColor.AQUA).event(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://discordapp.com/invite/TJ3bkkY"))
      .create()


  def splitToComponentTimes(biggy: BigDecimal): (Int, Int, Int) = {
    val long = biggy.longValue
    val hours = (long/3600).toInt
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
    Bukkit.broadcast(new TextComponent("テスト用メッセ"))
    Bukkit.broadcast(comp:_*)
  }
}
