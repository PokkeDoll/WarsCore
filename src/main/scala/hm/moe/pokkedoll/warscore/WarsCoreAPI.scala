package hm.moe.pokkedoll.warscore

import hm.moe.pokkedoll.warscore.events.PlayerUnfreezeEvent
import hm.moe.pokkedoll.warscore.games._
import hm.moe.pokkedoll.warscore.utils._
import net.md_5.bungee.api.chat.{BaseComponent, ClickEvent, ComponentBuilder, HoverEvent}
import org.bukkit._
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.{EntityType, Firework, Player}
import org.bukkit.inventory.{ItemFlag, ItemStack}
import org.bukkit.persistence.PersistentDataType
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.scoreboard.{DisplaySlot, Scoreboard, ScoreboardManager, Team}

import scala.annotation.tailrec
import scala.collection.mutable
import scala.util.Random

/**
 * 便利なメソッドをまとめたオブジェクト
 *
 * @author Emorard
 */
object WarsCoreAPI {
  val LOBBY = "p-lobby"

  lazy val scoreboardManager: ScoreboardManager = Bukkit.getScoreboardManager

  lazy val scoreboard: Scoreboard = scoreboardManager.getNewScoreboard

  lazy val random = new Random()

  protected[warscore] var DEFAULT_SPAWN: Location = _

  protected[warscore] var FIRST_SPAWN: Location = _

  /** データベース */
  private val database = WarsCore.instance.database

  /** ゲーム情報 */
  val games = mutable.HashMap.empty[String, Game]

  /** プレイヤーのキャッシュ */
  val wplayers = new mutable.HashMap[Player, WPlayer](50, 1.0)

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
            if (value.disconnect) {
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
   * ゲームの情報を読み込む
   *
   * @param cs game
   */
  def reloadGame(cs: ConfigurationSection): Unit = {
    games.clear()

    (1 to 4) foreach (id => {
      games.put(s"tdm-$id", new TeamDeathMatch(s"tdm-$id"))
      WorldLoader.asyncUnloadWorld(s"tdm-$id-0")
    })

    (1 to 2) foreach (id => {
      games.put(s"dom-$id", new Domination(s"dom-$id"))
      WorldLoader.asyncUnloadWorld(s"dom-$id-0")
    })

    games.put("tactics-1", new Tactics("tactics-1"))
    WorldLoader.asyncUnloadWorld("tactics-1-0")

    games.put("tdm4-1", new TeamDeathMatch4("tdm4-1"))
    WorldLoader.asyncUnloadWorld("tdm4-1-0")
  }

  /**
   * プレイヤーの所持している武器名を取得する
   *
   * @param player Player
   * @return
   */
  def getAttackerWeaponName(player: Player): Option[String] = {
    val item = player.getInventory.getItemInMainHand
    if (item == null || !item.hasItemMeta) None
    else {
      val meta = item.getItemMeta
      if (meta.hasDisplayName) Some(meta.getDisplayName) else Some(item.getType.toString)
    }
  }

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
        // RankManager.updateSidebar(scoreboard, data = rankData)

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

  def sendNews4User(player: Player): Unit = {
    player.sendMessage(
      new ComponentBuilder().append(createHeader("お知らせ"))
        .append("* ").reset().append("大規模更新中。裏のシステム以外は変わらない\n")
        .append("* ").reset().append("武器の性能を纏めました(12/25、自動生成)。閲覧するにはクリック！").event(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://docs.google.com/spreadsheets/d/1rcf_mYWRa-plweVOn-xSYNETjLKy7B_njrDc01g5ZIc/edit#gid=2015109485"))
        .create(): _*
    )
  }

  def sendNews4Staff(player: Player): Unit = {

  }

  /**
   * ヘッダーを作成する。改行もしてくれる
   *
   * @param string ヘッダーのタイトル
   * @param color  色
   * @return
   */
  def createHeader(string: String, color: ChatColor = ChatColor.GREEN): Array[BaseComponent] = {
    new ComponentBuilder("= = = = = = = = = =").color(color).underlined(true)
      .append(" " + string + " ").underlined(false)
      .append("= = = = = = = = = =").underlined(true)
      .append("\n").reset().create()
  }


  /**
   * 数字から時間を日本語にして返す
   *
   * @param biggy 試合の残り時間。秒
   * @return 時間、分、秒のタプル
   */
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

  def gameLog(gameid: String, level: String, message: String): Unit = {
    database.gameLog(gameid, level, message)
  }

  /**
   * ロビーのインベントリを退避する
   *
   * @version v1.3.16
   */
  def changeWeaponInventory(wp: WPlayer): Unit = {
    val player = wp.player
    database.setVInv(player.getUniqueId.toString, player.getInventory.getStorageContents, new Callback[Unit] {
      override def success(value: Unit): Unit = {
        val weapons = database.getActiveWeapon(player.getUniqueId.toString)
        val get = (key: String, default: String) => ItemUtil.getItem(key).getOrElse(ItemUtil.getItem(default).get)
        player.getInventory.setContents(
          Array(
            get(weapons._1, "ak-47"),
            get(weapons._2, "m92"),
            get(weapons._3, "knife"),
            get(weapons._4, "grenade"),
            new ItemStack(Material.AIR),
            new ItemStack(Material.AIR),
            new ItemStack(Material.AIR),
            new ItemStack(Material.AIR),
            new ItemStack(Material.CLOCK)
          ))
      }

      override def failure(error: Exception): Unit = {
        wp.sendMessage("ロビーインベントリの読み込みに失敗しました")
      }
    })
  }

  /**
   *
   * @version v1.3.16
   * @param player 対象のプレイヤー
   */
  def restoreLobbyInventory(player: Player): Unit = {
    database.getVInv(player.getUniqueId.toString, new Callback[mutable.Buffer[(Int, Array[Byte])]] {
      override def success(value: mutable.Buffer[(Int, Array[Byte])]): Unit = {
        val contents = Array.fill(36)(new ItemStack(Material.AIR))
        value.foreach(f => {
          contents(f._1) = ItemStack.deserializeBytes(f._2)
        })
        player.getInventory.setStorageContents(contents)
      }

      override def failure(error: Exception): Unit = {
        player.sendMessage("なんと復元できませんでした")
      }
    })
  }

  /**
   * ワールドの識別子を設定する
   *
   * @since v1.6.1
   * @return 0から999までの**文字列**を返す
   */
  def getWorldHash: String = random.nextInt(1000).toString

  /**
   * @since v1.6.1
   * @param game 対象のゲーム
   * @return
   */
  @tailrec
  def createWorldHash(game: Game): String = {
    val id = game.id + getWorldHash
    if (game.worldId == id)
      createWorldHash(game)
    else
      id
  }

  def getItemStackName(itemStack: ItemStack): String = {
    if (itemStack.hasItemMeta && itemStack.getItemMeta.hasDisplayName) {
      itemStack.getItemMeta.getDisplayName
    } else {
      itemStack.getType.toString.replaceAll("_", " ")
    }
  }

  def debug(player: Player, msg: String): Unit = {
    player.sendMessage(ChatColor.RED + "【デバッグ】 " + msg)
  }

  def info(player: Player, msg: String): Unit = {
    player.sendMessage(ChatColor.BLUE + "【情報】 " + msg)
  }

  def error(player: Player, msg: String): Unit = {
    player.sendMessage(ChatColor.RED + "【エラー】 " + msg)
  }

  val weaponUnlockNameKey = new NamespacedKey(WarsCore.instance, "weapon-unlock-name")
  val weaponUnlockTypeKey = new NamespacedKey(WarsCore.instance, "weapon-unlock-type")

  def unlockWeapon(player: Player, t: String, weapon: String): Unit = {
    database.addWeapon(player.getUniqueId.toString, t, weapon, 1)
  }

  def parseInt(string: String): Int = {
    try {
      string.toInt
    } catch {
      case _: NumberFormatException =>
        -1
    }
  }

  def getNamedItemStack(material: Material, name: String): ItemStack = {
    getNamedItemStack(material, name, java.util.Collections.emptyList())
  }

  def getNamedItemStack(material: Material, name: String, lore: java.util.List[String]): ItemStack = {
    val i = new ItemStack(material)
    i.addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
    val m = i.getItemMeta
    m.setDisplayName(name)
    m.setLore(lore)
    i.setItemMeta(m)
    i
  }

  /**
   * 共通して使えるUIを定義する
   *
   * @author Emorard
   */
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

    val CLOSE: ItemStack = {
      val i = new ItemStack(Material.BARRIER)
      val m = i.getItemMeta
      m.setDisplayName(ChatColor.RED + "インベントリを閉じる")
      i.setItemMeta(m)
      i
    }
  }
}
