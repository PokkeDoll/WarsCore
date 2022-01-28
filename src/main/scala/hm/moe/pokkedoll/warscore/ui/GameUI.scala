package hm.moe.pokkedoll.warscore.ui

import hm.moe.pokkedoll.warscore.WarsCoreAPI.{colorCode, games}
import hm.moe.pokkedoll.warscore.games.{Domination, Game, GameState, HardCoreGames, Tactics, TeamDeathMatch, TeamDeathMatch4}
import hm.moe.pokkedoll.warscore.{Registry, WarsCoreAPI}
import net.md_5.bungee.api.ChatColor
import org.bukkit.entity.{HumanEntity, Player}
import org.bukkit.event.inventory.{ClickType, InventoryClickEvent}
import org.bukkit.inventory.meta.{Damageable, ItemMeta}
import org.bukkit.inventory.{Inventory, ItemFlag, ItemStack}
import org.bukkit.persistence.PersistentDataType
import org.bukkit.{Bukkit, Material, Sound}

object GameUI {

  val GAME_INVENTORY_TITLE: String = ChatColor.GREEN + "ゲーム一覧！"

  val GAME_ROOM_TITLE: String = ChatColor.GREEN + "試合一覧！"

  val GAME_ROOM_SETTING_TITLE: String = ChatColor.RED + "試合設定"

  private val openGameInventoryIcon: ItemStack = {
    val i = new ItemStack(Material.ARROW, 1)
    val m = i.getItemMeta
    m.setUnbreakable(true)
    i.setItemMeta(m)
    i
  }

  private def createIcon(game: Game): ItemStack = {
    val i = new ItemStack(Material.STONE)
    i.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE)
    val m = i.getItemMeta
    m.getPersistentDataContainer.set(Registry.GAME_ID, PersistentDataType.STRING, game.id)
    val lore = new java.util.ArrayList[String]()
    game match {
      case _: TeamDeathMatch4 =>
        i.setType(Material.IRON_HOE)
        m.setCustomModelData(6)
        m.setDisplayName(colorCode("&aチームデスマッチ&c4"))
        lore.add(colorCode("&54対4でキル数を競います"))
        lore.add(colorCode("&5マップが狭いのでゲームの展開が速いです"))
      case _: TeamDeathMatch =>
        i.setType(Material.IRON_HOE)
        m.setCustomModelData(1)
        m.setDisplayName(colorCode("&aチームデスマッチ"))
        lore.add(colorCode("&510対10でキル数を競います"))
      case _: Tactics =>
        i.setType(Material.DIAMOND_SWORD)
        m.setCustomModelData(1)
        m.setDisplayName(colorCode("&aタクティクス"))
        lore.add(colorCode("&51対1で3本先取で争います"))
      case _: Domination =>
        i.setType(Material.BEACON)
        m.setDisplayName(colorCode("&aドミネーション"))
        lore.add("&6:o")
      case _: HardCoreGames =>
        i.setType(Material.IRON_SHOVEL)
        m.setDisplayName(colorCode("ハードコアゲームズ"))
        lore.add(colorCode("3人のチームを組み，最後まで生き残りましょう"))
    }
    lore.add(colorCode("&7= = = = = = = ="))
    game.state match {
      case GameState.WAIT | GameState.STANDBY =>
        lore.add(colorCode("&7状態: &a待機中"))
        lore.add(colorCode(s"&7* &a${game.mapInfo.mapName}"))
        lore.add(colorCode(s"&7* &a${game.members.size} &7/ &a${game.maxMember} プレイ中"))
      case GameState.READY =>
        lore.add(colorCode("&7状態: &a準備中"))
        lore.add(colorCode(s"&7*: &a${game.mapInfo.mapName}"))
        lore.add(colorCode(s"&7* &a${game.members.size} &7/ &a${game.maxMember} プレイ中"))
      case GameState.PLAY | GameState.PLAYING =>
        lore.add(colorCode("&7状態: &a試合中！"))
        lore.add(colorCode(s"&7*: &a${game.mapInfo.mapName}"))
        lore.add(colorCode(s"&7* &a${game.members.size} &7/ &a${game.maxMember} プレイ中"))
      case GameState.PLAY2 | GameState.PLAYING_CANNOT_JOIN =>
        lore.add(colorCode("&7状態: &c試合中(参加できません)"))
        lore.add(colorCode(s"&7*: &a${game.mapInfo.mapName}"))
        lore.add(colorCode(s"&7* &a${game.members.size} &7/ &a${game.maxMember} プレイ中"))
      case GameState.INIT =>
        lore.add(colorCode("&7状態: &e初期化中..."))
      case GameState.END =>
        lore.add(colorCode("&7状態: &e終了"))
      case GameState.ERROR =>
        lore.add(colorCode("&7状態: &dマップの読み込みに失敗したため停止しました"))
      case GameState.FREEZE =>
        lore.add(colorCode("&7状態: &9参加できません"))
      case GameState.LOADING_WORLD =>
        lore.add(colorCode("わ"))
      case _ =>
        lore.add(colorCode("&7状態: 無効"))
        lore.add(colorCode("&b&lクリックして部屋を作成します"))
    }
    m.setLore(lore)
    i.setItemMeta(m)
    i
  }

  /**
   * ゲームインベントリを開く
   *
   * @since v1.4.1
   * @param player 対象のプレイヤー
   */
  def openMainUI(player: HumanEntity): Unit = {
    val inv = Bukkit.createInventory(null, 18, GAME_INVENTORY_TITLE)
    inv.setItem(0, openGameInventoryIcon)
    inv.setItem(9, new ItemStack(Material.IRON_SWORD))

    val tdm = games.getOrElse("tdm-1", return)
    val tdm4 = games.getOrElse("tdm4-1", return)
    val tac = games.getOrElse("tactics-1", return)
    val dom = games.getOrElse("dom-1", return)
    val hcg = games.getOrElse("hcg", return)

    inv.setContents(Array.fill(18)(WarsCoreAPI.UI.PANEL))

    inv.setItem(3, createIcon(tdm))
    inv.setItem(5, createIcon(tdm4))
    inv.setItem(10, createIcon(tac))
    inv.setItem(13, createIcon(hcg))
    inv.setItem(16, createIcon(dom))

    player.openInventory(inv)
  }

  def openGameRoomUI(player: HumanEntity, gameId: String): Unit = {
    val games = WarsCoreAPI.games.filter(p => p._1.startsWith(gameId)).values.toSeq
    val inv = Bukkit.createInventory(null, 9)
    games.indices.foreach(i => {
      inv.setItem(i, createIcon(games(i)))
    })
    player.openInventory(inv)
  }

  def clickGameRoomUI(e: InventoryClickEvent): Unit = {
    e.getWhoClicked match {
      case player: Player =>
        e.setCancelled(true)
        val icon = e.getCurrentItem
        if (icon == null || !icon.hasItemMeta || !icon.getItemMeta.hasDisplayName) return
        WarsCoreAPI.games.get(ChatColor.stripColor(icon.getItemMeta.getDisplayName)) match {
          case Some(game) =>
            e.getClick match {
              // 設定画面へGo
              case ClickType.RIGHT =>
                openGameSettingUI(player, game)
              case _ =>
                // game.join(player)
                Game.join(WarsCoreAPI.getWPlayer(player), game)
                player.closeInventory()
            }
          case None =>
            player.sendMessage(ChatColor.RED + "ゲームが見つかりませんでした。運営に連絡してください。")
        }
    }
  }

  val maxTimeIcon: Int => ItemStack = (s: Int) => {
    val i = new ItemStack(Material.CLOCK)
    val m = i.getItemMeta
    m.setDisplayName(ChatColor.GREEN + "制限時間: " + ChatColor.YELLOW + s / 60 + "分")
    m.setLore(java.util.Arrays.asList(
      colorCode("&5試合の制限時間を設定します"),
      colorCode("&7【&3左クリック&7】 &e1分減らす"),
      colorCode("&7【&3右クリック&7】 &e1分増やす"),
    ))
    i.setItemMeta(m)
    i
  }

  val coldGameIcon: Boolean => ItemStack = (bool: Boolean) => {
    val i = new ItemStack(Material.LIME_WOOL)
    val m = i.getItemMeta
    if (bool) {
      m.setDisplayName(colorCode("&aコールドゲームが有効"))
      m.setLore(java.util.Arrays.asList(colorCode("&5大差がついた場合、残り時間に関わらずゲームが終了する"), colorCode("&7【&3クリック&7】&e無効にする")))
    } else {
      i.setType(Material.RED_WOOL)
      m.setDisplayName(colorCode("&cコールドゲームが無効"))
      m.setLore(java.util.Arrays.asList(colorCode("&5大差がついた場合、残り時間に関わらずゲームが終了する"), colorCode("&7【&3クリック&7】&e有効にする")))
    }
    i.setItemMeta(m)
    i
  }


  def openGameSettingUI(player: Player, game: Game): Unit = {
    val inv = Bukkit.createInventory(null, 9, GAME_ROOM_SETTING_TITLE)
    game.isSetting = true
    // 割と重要
    val close = {
      val i = new ItemStack(Material.BARRIER)
      val m = i.getItemMeta
      m.getPersistentDataContainer.set(Registry.GAME_ID, PersistentDataType.STRING, game.id)
      i.setItemMeta(m)
      i
    }
    inv.setItem(0, close)
    game match {
      case tdm: TeamDeathMatch =>
        inv.setItem(1, maxTimeIcon(tdm.maxTime))
        inv.setItem(2, coldGameIcon(tdm.isToggleColdGame))
      case _ =>
    }
    player.openInventory(inv)
  }

  val TIME_INVENTORY_TITLE: String = colorCode("&c試合時間を変更します")

  private val currentTimeIcon = (game: Game) => {
      val item = new ItemStack(Material.FILLED_MAP)
      val meta = item.getItemMeta
      meta.setDisplayName(colorCode(s"&a現在の試合時間: ${WarsCoreAPI.splitToComponentTimes(game.maxTime)._2}分"))
      meta.getPersistentDataContainer.set(Registry.GAME_ID, PersistentDataType.STRING, game.id)
      item.setItemMeta(meta)
      item
    }


  val timeInventory: Game => Inventory = (game: Game) => {
    val inv = Bukkit.createInventory(null, 9, TIME_INVENTORY_TITLE)
    inv.setItem(0, currentTimeIcon(game))
    (1 to 9).foreach(i => {
      val item = new ItemStack(Material.PAPER, i)
      val meta = item.getItemMeta
      val timeComponent = WarsCoreAPI.splitToComponentTimes(60 + (i * 60))
      meta.setDisplayName(colorCode(s"&a${timeComponent._2}分"))
      meta.setLore(java.util.Arrays.asList(colorCode("&cクリックして変更！")))
      item.setItemMeta(meta)
      inv.setItem(i, item)
    })
    inv
  }

  def onClickTimeInventory(inv: Inventory, slot: Int, player: Player): Unit = {
    val item = inv.getItem(0)
    if(item != null && item.getType != Material.AIR && item.hasItemMeta) {
      WarsCoreAPI.games.get(item.getItemMeta.getPersistentDataContainer.get(Registry.GAME_ID, PersistentDataType.STRING)) match {
        case Some(game) if slot > 0 && slot < 9 =>
          game.maxTime = 60 + slot * 60
          player.playSound(player.getLocation, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f)
          inv.setItem(0, currentTimeIcon(game))
        case _ =>
      }
    }
  }

  val TDM_UI: ItemStack = {
    val i = new ItemStack(Material.IRON_HOE)
    val damageable = i.getItemMeta.asInstanceOf[Damageable]
    damageable.setDamage(1)
    val meta = i.getItemMeta
    meta.setCustomModelData(1)
    meta.setUnbreakable(true)
    meta.setDisplayName(colorCode("&aTeam Death Match"))
    meta.setLore(java.util.Arrays.asList(
      colorCode("&510対10に分かれてキル数を争います"),
    ))
    i.setItemMeta(damageable.asInstanceOf[ItemMeta])
    i.setItemMeta(meta)
    i
  }

  val TDM4_UI: ItemStack = {
    val i = new ItemStack(Material.IRON_HOE)
    val damageable = i.getItemMeta.asInstanceOf[Damageable]
    damageable.setDamage(6)
    val meta = i.getItemMeta
    meta.setCustomModelData(6)
    meta.setUnbreakable(true)
    meta.setDisplayName(colorCode("&aTeam Death Match &c4"))
    meta.setLore(java.util.Arrays.asList(
      colorCode("&54対4に分かれてキル数を争います"),
    ))
    i.setItemMeta(damageable.asInstanceOf[ItemMeta])
    i.setItemMeta(meta)
    i
  }
}
