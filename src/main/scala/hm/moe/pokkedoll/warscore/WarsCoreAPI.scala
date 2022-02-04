package hm.moe.pokkedoll.warscore

import hm.moe.pokkedoll.warscore.games._
import hm.moe.pokkedoll.warscore.utils._
import hm.moe.pokkedoll.warsgame.PPEX
import net.md_5.bungee.api.chat.{BaseComponent, ClickEvent, ComponentBuilder, HoverEvent}
import org.bukkit._
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.{EntityType, Firework, Player}
import org.bukkit.inventory.meta.LeatherArmorMeta
import org.bukkit.inventory.{ItemFlag, ItemStack}
import org.bukkit.persistence.PersistentDataType
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.scoreboard._

import scala.collection.mutable
import scala.util.Random

/**
 * 便利なメソッドをまとめたオブジェクト
 *
 * @author Emorard
 */
object WarsCoreAPI {

  val VERSION = "2.3"

  lazy val scoreboardManager: ScoreboardManager = Bukkit.getScoreboardManager

  // プレイヤー個人が持つ唯一のスコアボード
  lazy val scoreboard: Scoreboard = scoreboardManager.getNewScoreboard

  // スコアボードのキャッシュ
  val scoreboards = mutable.HashMap.empty[Player, Scoreboard]

  lazy val random = new Random()

  //TODO 脆弱！堅牢にするべき
  var DEFAULT_SPAWN: Location = _

  var FIRST_SPAWN: Location = _

  /** データベース */
  private val database = WarsCore.instance.database

  /** ゲーム情報 */
  val games = mutable.HashMap.empty[String, Game]

  /** プレイヤーのキャッシュ */
  val wplayers = new mutable.HashMap[Player, WPlayer](50, 1.0)

  /** カラーコード */
  val colorCode: String => String = (string: String) => ChatColor.translateAlternateColorCodes('&', string)


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
  def getWPlayer(player: Player): WPlayer = wplayers.getOrElseUpdate(player, {
      val wp = new WPlayer(player)
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
    })

  /**
   * ゲームの情報を読み込む
   *
   * @param cs game
   */
  def reloadGame(cs: ConfigurationSection): Unit = {
    games.clear()
    games.put("tdm-1", new TeamDeathMatch("tdm-1"))
    WorldLoader.asyncUnloadWorld("tdm-1")

    games.put(s"dom-1", new Domination(s"dom-1"))
    WorldLoader.asyncUnloadWorld(s"dom-1")

    games.put(s"tactics-1", new Tactics(s"tactics-1"))
    WorldLoader.asyncUnloadWorld(s"tactics-1")

    games.put(s"tdm4-1", new TeamDeathMatch4(s"tdm4-1"))
    WorldLoader.asyncUnloadWorld(s"tdm4-1")

    games.get("dom-1").foreach(_.state = GameState.FREEZE)

    games.put("hcg-1", new HardCoreGames("hcg-1"))

    games.put("ppex-1", new PPEX("ppex-1"))
    WorldLoader.asyncUnloadWorld(s"ppex-1")
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
      Some(if(meta.hasDisplayName) meta.getDisplayName else item.getType.toString)
    }
  }

  /**
   * スコアボード(主にネームタグ)を更新する
   *
   * @param player Player
   */
  def updateNameTag(player: Player, scoreboard: Scoreboard): Unit = {
    wplayers.get(player).foreach(wp => {
      val name = player.getName
      val team = Option(scoreboard.getTeam(name)).getOrElse({
        val team = scoreboard.registerNewTeam(name)
        team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS)
        team.addEntry(name)
        team
      })
      val prefix = colorCode(s"&7[&a${wp.rank}&7]&r ")
      val suffix = colorCode(s" &7[${RankManager.getClassName(wp.rank)}&7]")
      team.setPrefix(prefix)
      team.setSuffix(suffix)
      player.setScoreboard(scoreboard)
      // スコアボードの更新
      scoreboards.filterNot(_._1 == player).foreach(other => {
        WarsCore.instance.getLogger.info(s"WarsCore.updateNameTag($name)")

        /* 相手 -> 自分の更新 */
        val otherTeam = Option(other._2.getTeam(name)).getOrElse(other._2.registerNewTeam(name))
        otherTeam.setPrefix(prefix)
        otherTeam.setSuffix(suffix)
        otherTeam.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS)
        otherTeam.addEntry(name)

        /* 自分 => 相手の更新 */
        val myTeam = Option(scoreboard.getTeam(other._1.getName)).getOrElse(scoreboard.registerNewTeam(other._1.getName))
        myTeam.setPrefix(prefix)
        myTeam.setSuffix(suffix)
        myTeam.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS)
        myTeam.addEntry(other._1.getName)
      })
    })
  }


  def updateSidebar(player: Player, scoreboard: Scoreboard): Unit = {
    wplayers.get(player).foreach(wp => {
      if (scoreboard.getObjective(DisplaySlot.SIDEBAR) != null) scoreboard.getObjective(DisplaySlot.SIDEBAR).unregister()
      val obj = scoreboard.registerNewObjective("sidebar", "dummy", colorCode(s"&aWars &ev$VERSION"))
      obj.setDisplaySlot(DisplaySlot.SIDEBAR)

      val rank = wp.rank
      setSidebarContents(obj,
        List(
          //s"&9Rank&7: &a$rank",
          //s"&9Class&7: &r${RankManager.getClassName(rank)}",
          //s"&9EXP&7: &a${wp.exp} &7/ &a${RankManager.nextExp(rank)}",
          //" ",
          s"&e/pp &fメニューを開く", "&e/wp &f武器を設定する",
          "&e/game &fゲームをする")
          .map(colorCode))
    })
  }

  def setSidebarContents(obj: Objective, list: List[String]): Unit = {
    list.indices.reverse.foreach(i => obj.getScore(list(i)).setScore(list.length - i))
  }


  def addScoreBoard(player: Player): Unit = {
    val s = scoreboards.getOrElseUpdate(player, scoreboardManager.getNewScoreboard)
    updateNameTag(player, s)
    updateSidebar(player, s)
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
        .append(ChatColor.GREEN + "06/21: 投票が自動でカウントされるように.  /voteで確認できます.")
        .create(): _*
    )
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
        WarsCore.instance.getLogger.warning(s"${e.getMessage} at WarsCoreAPI.getLocation($string)")
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

  /**
   * ロビーのインベントリを退避する
   *
   * @version v1.3.16
   */
  def changeWeaponInventory(wp: WPlayer): Unit = {
    val player = wp.player
    database.setVInv(player.getUniqueId.toString, player.getInventory.getStorageContents, new Callback[Unit] {
      override def success(value: Unit): Unit = {
        setActiveWeapons(player)
      }

      override def failure(error: Exception): Unit = {
        wp.sendMessage("ロビーインベントリの読み込みに失敗しました")
      }
    })
  }

  def setActiveWeapons(player: Player): Unit = {
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
    player.getInventory.setHelmet(ItemUtil.getItem(weapons._5).getOrElse(new ItemStack(Material.AIR)))
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

  val BLUE_CHESTPLATE: ItemStack = {
    val armor = new ItemStack(Material.LEATHER_CHESTPLATE)
    val meta = armor.getItemMeta.asInstanceOf[LeatherArmorMeta]
    meta.setColor(Color.BLUE)
    armor.setItemMeta(meta)
    armor
  }

  val RED_CHESTPLATE: ItemStack = {
    val armor = new ItemStack(Material.LEATHER_CHESTPLATE)
    val meta = armor.getItemMeta.asInstanceOf[LeatherArmorMeta]
    meta.setColor(Color.RED)
    armor.setItemMeta(meta)
    armor
  }

  /**
   * つまり、観戦者 -> 非観戦者のマップ
   */
  private val cache = mutable.HashMap.empty[Player, Player]

  /**
   * 安全に(?)観戦するメソッド。<br>
   * 観戦者の観戦者を最初に習得し、観戦を外してキャッシュから削除する。<br>
   * その後に観戦を行うようにする。
   *
   * @param spectator 観戦を行うプレイヤー
   * @param target    観戦されるプレイヤー
   */
  // TODO バグあり、修正予定
  def spectate(spectator: Player, target: Player): Unit = {
    cache.filter(pred => pred._1.getGameMode == GameMode.SPECTATOR && pred._2 == spectator).foreach(f => {
      f._1.setSpectatorTarget(null)
      cache.remove(f._1)
    })
    spectator.setSpectatorTarget(target)
    cache.put(spectator, target)
  }

  /**
   * プレイヤーが試合終了時の自動参加を行うかを返す
   *
   * @param player 対象のプレイヤー
   * @return
   */
  def isContinue(player: Player): Boolean = {
    val meta = player.getMetadata("wc-continue")
    if (meta.isEmpty) true else meta.get(0).asBoolean()
  }

  def getGameMVP[T <: GamePlayerData](data: mutable.Map[Player, T]): Array[BaseComponent] = {
    val kd = data.filterNot(f => f._2.kill == 0 && f._2.death == 0).map(f => (f._1, f._2.kill, f._2.death, if (f._2.death == 0) f._2.kill.toDouble else f._2.kill / f._2.death.toDouble)).toSeq.sortBy(f => f._4).reverse.take(5)
    val dd = data.map(f => (f._1, f._2.damage)).toSeq.sortBy(f => f._2).reverse.take(5)

    val comp = new ComponentBuilder()
    comp.append(createHeader("MVP"))
    comp.append("\n").reset()
    comp.append(": K/D :==========>\n").color(ChatColor.YELLOW)
    kd.indices.foreach(i => {
      val v = kd(i)
      i + 1 match {
        case 1 =>
          comp.append("1st").color(ChatColor.YELLOW).bold(true).reset()
        case 2 =>
          comp.append("2nd").color(ChatColor.YELLOW).reset()
        case 3 =>
          comp.append("3rd").color(ChatColor.GREEN).reset()
        case _ =>
          comp.append(s"${i + 1}th").reset()
      }
      comp.append(".").color(ChatColor.GRAY).bold(false)
        .append(s" ${v._1.getName} ${BigDecimal(v._4).setScale(2, BigDecimal.RoundingMode.HALF_UP)}").color(ChatColor.YELLOW).append(s"(${v._2} / ${v._3})\n").color(ChatColor.GRAY).reset()
    })

    comp.append("\n").reset()
    comp.append(": Damage :==========>\n").color(ChatColor.YELLOW)
    dd.indices.foreach(i => {
      val v = dd(i)
      i + 1 match {
        case 1 =>
          comp.append("1st").color(ChatColor.YELLOW).bold(true)
        case 2 =>
          comp.append("2nd").color(ChatColor.YELLOW)
        case 3 =>
          comp.append("3rd").color(ChatColor.GREEN)
        case _ =>
          comp.append(s"${i + 1}th")
      }
      comp.append(".").color(ChatColor.GRAY).bold(false)
        .append(s" ${v._1.getName} ${v._2}\n").color(ChatColor.YELLOW).reset()
    })
    comp.create()
  }

  def getWeaponTypeFromLore(item: ItemStack): String = {
    if (item.hasItemMeta && item.getItemMeta.hasLore) {
      item.getItemMeta.getLore.forEach(lore => {
        if (lore.contains(colorCode("&e&lPrimary"))) {
          return "primary"
        } else if (lore.contains(colorCode("&e&lSecondary"))) {
          return "secondary"
        } else if (lore.contains(colorCode("&e&lMelee"))) {
          return "melee"
        } else if (lore.contains(colorCode("&e&lItem")) || lore.contains(colorCode("&e&lGrenade"))) {
          return "grenade"
        }
      })
    }
    ""
  }

  val loadingFont: Array[String] = Array("◜", "◝", "◞", "◟")

  def getLoadingTitleTask(game: Game): BukkitRunnable = {
    new BukkitRunnable {
      var i = 0
      override def run(): Unit = {
        if(game.state == GameState.WAIT || game.state == GameState.READY) {
          if(i > 3) i = 0
          game.members.map(_.player).foreach(_.sendTitle(ChatColor.GREEN + loadingFont(i), if(game.state == GameState.WAIT) ChatColor.GREEN + s"待機中..." else ChatColor.GREEN + "まもなく試合が始まります！", 0, 10, 0))
          i += 1
        } else {
          cancel()
        }
      }
    }
  }

  def showTitle(): Unit = {

  }

  def createRunnable(): Unit = {
    val a: BukkitRunnable = () => {

    }


  }


  //TODO val mutableをvar immutableに変更する。参照とオブジェクトを間違えてはいけない！

  /**
   * 共通して使えるUIを定義する
   *
   * @author Emorard
   */
  object UI {
    @Deprecated
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
