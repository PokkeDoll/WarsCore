package hm.moe.pokkedoll.warscore.ui

import hm.moe.pokkedoll.warscore.{Registry, WarsCoreAPI}
import hm.moe.pokkedoll.warscore.WarsCoreAPI.{colorCode, games}
import hm.moe.pokkedoll.warscore.games.{Game, GameState, TeamDeathMatch}
import net.md_5.bungee.api.ChatColor
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.{HumanEntity, Player}
import org.bukkit.event.inventory.{ClickType, InventoryAction, InventoryClickEvent, InventoryCloseEvent}
import org.bukkit.inventory.{ItemFlag, ItemStack}
import org.bukkit.persistence.PersistentDataType
import org.bukkit.{Bukkit, Material}

import java.util.UUID
import scala.collection.mutable

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
    val m = i.getItemMeta
    game.state match {
      case GameState.PLAY | GameState.WAIT | GameState.READY =>
        i.setType(Material.LIME_DYE)
        i.setAmount(if (game.members.isEmpty) 1 else game.members.size)
        m.setDisplayName(ChatColor.GREEN + game.id)
        m.setLore(java.util.Arrays.asList(
          ChatColor.LIGHT_PURPLE + game.title,
          ChatColor.GREEN + game.mapInfo.mapName,
          ChatColor.GREEN + (if (game.state == GameState.PLAY) "試合中！" else if (game.state == GameState.WAIT) s"${game.members.size} / ${game.maxMember} 待機中！" else s"${game.members.size} / ${game.maxMember} 準備中！"),
        ))
      case GameState.PLAY2 =>
        i.setType(Material.GRAY_DYE)
        m.setDisplayName(ChatColor.GRAY + game.id)
        m.setLore(java.util.Arrays.asList(
          ChatColor.LIGHT_PURPLE + game.title,
          ChatColor.GREEN + game.mapInfo.mapName,
          ChatColor.GREEN + (if (game.state == GameState.PLAY) "試合中！" else if (game.state == GameState.WAIT) s"${game.members.size} / ${game.maxMember} 待機中！" else s"${game.members.size} / ${game.maxMember} 準備中！"),
          ChatColor.RED + "もうすぐ試合が終わるので参加できません！"
        ))
      case GameState.INIT =>
        i.setType(Material.PINK_DYE)
        m.setDisplayName(ChatColor.LIGHT_PURPLE + game.id)
        m.setLore(java.util.Arrays.asList(
          ChatColor.LIGHT_PURPLE + game.title,
          ChatColor.WHITE + "初期化中..."
        ))
      case GameState.ERROR =>
        i.setType(Material.PINK_DYE)
        m.setDisplayName(ChatColor.LIGHT_PURPLE + game.id)
        m.setLore(java.util.Arrays.asList(
          ChatColor.LIGHT_PURPLE + game.title,
          ChatColor.RED + "マップの読込に失敗しました"
        ))
      case GameState.END =>
        i.setType(Material.GRAY_DYE)
        m.setDisplayName(ChatColor.GRAY + game.id)
        m.setLore(java.util.Arrays.asList(
          ChatColor.LIGHT_PURPLE + game.title,
          ChatColor.YELLOW + "試合が終了しました"
        ))
      case _ =>
        i.setType(Material.GRAY_DYE)
        m.setDisplayName(ChatColor.GRAY + game.id)
        m.setLore(java.util.Arrays.asList(
          ChatColor.LIGHT_PURPLE + game.title,
          ChatColor.RED + "クリックして部屋を作成します"
        ))
    }
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
    val inv = Bukkit.createInventory(null, 36, GAME_INVENTORY_TITLE)
    inv.setItem(0, openGameInventoryIcon)
    inv.setItem(9, new ItemStack(Material.IRON_SWORD))
    val g = games.groupBy(f => f._2.title)
    var slot = 0
    g.foreach(f => {
      var s = slot
      f._2.foreach(ff => {
        inv.setItem(s, createIcon(ff._2))
        s += 1
      })
      slot += 9
    })
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
                game.join(player)
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

  def clickGameSettingUI(e: InventoryClickEvent): Unit = {
    val player = e.getWhoClicked
    val inv = e.getClickedInventory

    val close = inv.getItem(0).getItemMeta.getPersistentDataContainer
    val gameId = close.get(Registry.GAME_ID, PersistentDataType.STRING)
    WarsCoreAPI.games.get(gameId) match {
      case Some(game) =>
        game match {
          case tdm: TeamDeathMatch =>
            e.getSlot match {
              case 0 =>
                game.isSetting = false
                player.closeInventory()
              case 1 =>
                e.getClick match {
                  case ClickType.LEFT =>
                    game.maxTime = Math.min(180, game.maxTime - 60)
                  case ClickType.RIGHT =>
                    game.maxTime = Math.max(900, game.maxTime + 60)
                  case _ =>
                }
                inv.setItem(1, maxTimeIcon(game.maxTime))
              case 2 =>
                tdm.isToggleColdGame = !tdm.isToggleColdGame
                inv.setItem(2, coldGameIcon(tdm.isToggleColdGame))
            }
        }
      case None =>
        player.sendMessage(ChatColor.RED + "エラーが発生しました: 無効なゲームID")
    }
  }

  def closeGameSettingUI(e: InventoryCloseEvent): Unit = {
    val close = e.getInventory.getItem(0)
    if(close != null) {
      val gameId = close.getItemMeta.getPersistentDataContainer.get(Registry.GAME_ID, PersistentDataType.STRING)
      WarsCoreAPI.games.get(gameId) match {
        case Some(game) => game.isSetting = false
        case None =>
      }
    }
  }
}
