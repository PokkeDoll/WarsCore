package hm.moe.pokkedoll.warscore.db

import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import hm.moe.pokkedoll.warscore.games.TeamDeathMatch
import hm.moe.pokkedoll.warscore.utils.{Item, ItemUtil, ShopUtil}
import hm.moe.pokkedoll.warscore.{Callback, WPlayer, WarsCore, WarsCoreAPI}
import net.md_5.bungee.api.ChatColor
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.BukkitRunnable

import java.sql.{ResultSet, SQLException}
import java.util
import scala.collection.mutable
import scala.jdk.CollectionConverters.IterableHasAsJava
import scala.util.{Failure, Success, Try, Using}

/**
 * SQLite3でのDatabase実装
 */
class SQLite(private val plugin: WarsCore) extends Database {

  private val path = "database.db"
  private val config = new HikariConfig()

  config.setDriverClassName("org.sqlite.JDBC")
  config.setJdbcUrl(s"jdbc:sqlite:${plugin.getDataFolder.getAbsolutePath}/$path")
  config.setConnectionInitSql("SELECT 1")

  val hikari = new HikariDataSource(config)

  /**
   * データベースに自分のデータがあるか確認するメソッド
   *
   * @since v1.4.1
   * @param uuid 対象のUUID
   * @return UUIDが存在すればtrue
   */
  override def hasUUID(uuid: String): Boolean = {
    Using.Manager { use =>
      val c = use(hikari.getConnection())
      val ps = use(c.prepareStatement("SELECT uuid FROM player WHERE uuid=?"))
      ps.setString(1, uuid)
      val rs = ps.executeQuery()
      rs.next()
    }.getOrElse(false)
  }

  /**
   * データを登録するメソッド
   *
   * @param uuid UUID
   * @return
   */
  override def insert(uuid: String): Boolean = {
    Using.Manager { use =>
      val c = use(hikari.getConnection())
      val s = use(c.createStatement())
      s.addBatch(s"INSERT INTO player(uuid) VALUES('$uuid')")
      s.addBatch(s"INSERT INTO rank(uuid) VALUES('$uuid')")
      s.addBatch(s"INSERT INTO tag(uuid, use) VALUES('$uuid', 1)")
      s.addBatch(s"INSERT INTO tdm(uuid) VALUES('$uuid')")
      s.addBatch(s"INSERT INTO tactics(uuid) VALUES('$uuid')")
      s.executeBatch()
    }.isSuccess
  }

  override def getRankData(uuid: String): Option[(Int, Int)] = {
    Using.Manager { use =>
      val c = use(hikari.getConnection())
      val ps = use(c.prepareStatement("SELECT id, exp FROM rank WHERE uuid=?"))
      ps.setString(1, uuid)
      val rs = ps.executeQuery()
      if (rs.next()) {
        Some(rs.getInt("id"), rs.getInt("exp"))
      } else {
        None
      }
    }.getOrElse(None)
  }

  override def setRankData(uuid: String, data: (Int, Int)): Unit = {
    Using.Manager { use =>
      val c = use(hikari.getConnection())
      val ps = use(c.prepareStatement("UPDATE rank SET id=?, exp=? WHERE uuid=?"))
      ps.setInt(1, data._1)
      ps.setInt(2, data._2)
      ps.setString(3, uuid)
      ps.executeUpdate()
    }
  }

  override def updateTDM(game: TeamDeathMatch): Boolean = {
    Using.Manager { use =>
      val c = use(hikari.getConnection())
      val ps = use(c.prepareStatement("UPDATE tdm SET kill=kill+?, death=death+?, assist=assist+?, damage=damage+?, damaged=damaged+?, win=win+?, play=play+1 WHERE uuid=?"))
      game.data.map(f => (f._1.getUniqueId.toString, f._2)).foreach(f => {
        ps.setInt(1, f._2.kill)
        ps.setInt(2, f._2.death)
        ps.setInt(3, f._2.assist)
        ps.setInt(4, f._2.damage.toInt)
        ps.setInt(5, f._2.damaged.toInt)
        ps.setInt(6, if (f._2.win) 1 else 0)
        ps.setString(7, f._1)
        ps.executeUpdate()
      })
    }.isSuccess
  }

  /**
   * ゲームのログを設定する
   * @param game ゲームID
   * @param reason 記録される理由
   * @param message 内容
   */
  override def gameLog(game: String, reason: String, message: String): Try[Unit] = {
    Using.Manager { use =>
      val c = use(hikari.getConnection())
      val s = use(c.createStatement())
      s.executeUpdate(s"INSERT INTO gamelog VALUES(date(), '$game', '$reason', '$message')")
    }
  }


  /**
   * 仮想インベントリを読み込む
   *
   * @param wp
   * @param col normalまたはgame
   */
  override def getVInventory(wp: WPlayer, col: String): Unit = {
    val c = hikari.getConnection
    val ps = c.prepareStatement("SELECT ? FROM `vinv` WHERE `uuid`=?")
    try {
      ps.setString(1, col)
      ps.setString(2, wp.player.getUniqueId.toString)
      val rs = ps.executeQuery()
      if (rs.next()) {
        rs.getString(col)
      }
    } catch {
      case e: SQLException =>
        e.printStackTrace()
    } finally {
      ps.close()
      c.close()
    }
  }

  /**
   * 仮想インベントリをセーブする
   *
   * @param wp
   * @param col normalまたはgame
   */
  override def setVInventory(wp: WPlayer, col: String): Unit = ???

  /**
   * WPプレイヤーの保存データを読み込む
   *
   * @version 2.0
   * @since v1.1.18
   * @param wp       対象のプレイヤー
   * @param callback 取得したデータを同期的に返す
   */
  override def loadWPlayer(wp: WPlayer, callback: Callback[WPlayer]): Unit = {
    new BukkitRunnable {
      override def run(): Unit = {
        val c = hikari.getConnection
        //val ps = c.prepareStatement("SELECT player.uuid, player.disconnect, rank.id, rank.exp, tag.tagId FROM player JOIN rank ON player.uuid=rank.uuid JOIN tag ON player.uuid=tag.uuid and tag.use=1 WHERE player.uuid=?")
        val ps = c.prepareStatement("SELECT player.uuid, player.disconnect, rank.id, rank.exp FROM player JOIN rank ON player.uuid=rank.uuid WHERE player.uuid=?")
        try {
          ps.setString(1, wp.player.getUniqueId.toString)
          val rs = ps.executeQuery()

          if (rs.next()) {
            wp.rank = rs.getInt("id")
            wp.exp = rs.getInt("exp")
            //wp.tag = rs.getString("tagId")
            wp.tag = ""
            wp.disconnect = ((i: Int) => if (i == 1) true else false) (rs.getInt("disconnect"))
          } else {
            wp.rank = -1
            wp.exp = -1
            wp.tag = ""
            wp.disconnect = false
          }
          new BukkitRunnable {
            override def run(): Unit = {
              callback.success(wp)
            }
          }.runTask(plugin)
        } catch {
          case e: SQLException =>
            new BukkitRunnable {
              override def run(): Unit = {
                callback.failure(e)
              }
            }.runTask(plugin)
        } finally {
          ps.close()
          c.close()
        }
      }
    }.runTaskAsynchronously(plugin)
  }


  override def close(): Unit = hikari.close()

  /**
   * 仮のインベントリ(ロビーのインベントリを取得する
   *
   * @version v1.3.15
   * @param uuid     対象のUUID
   * @param callback (スロット番号, シリアライズされたアイテムスタック)のタプル
   */
  override def getVInv(uuid: String, callback: Callback[mutable.Buffer[(Int, Array[Byte])]]): Unit = {
    new BukkitRunnable {
      override def run(): Unit = {
        val c = hikari.getConnection
        val ps = c.prepareStatement("SELECT slot, data FROM vinv WHERE uuid=?")
        try {
          ps.setString(1, uuid)
          val rs = ps.executeQuery()
          var buffer = mutable.Buffer.empty[(Int, Array[Byte])]
          while (rs.next()) {
            buffer.+=((rs.getInt("slot"), rs.getBytes("data")))
          }
          new BukkitRunnable {
            override def run(): Unit = {
              callback.success(buffer)
            }
          }.runTask(plugin)
        } catch {
          case e: SQLException =>
            new BukkitRunnable {
              override def run(): Unit = {
                callback.failure(e)
              }
            }.runTask(plugin)
        } finally {
          ps.close()
          c.close()
        }
      }
    }.runTaskAsynchronously(plugin)
  }

  /**
   * ロビーのインベントリを退避する
   *
   * @version v1.3.15
   * @param uuid
   * @param contents
   */
  override def setVInv(uuid: String, contents: Array[ItemStack], callback: Callback[Unit]): Unit = {
    if (contents.isEmpty) {
      callback.success()
    } else {
      new BukkitRunnable {
        override def run(): Unit = {
          val c = hikari.getConnection
          val s = c.createStatement()
          val ps = c.prepareStatement("INSERT INTO vinv VALUES(?, ?, ?)")
          try {
            s.executeUpdate(s"DELETE FROM vinv WHERE uuid='$uuid'")
            ps.setString(1, uuid)
            contents.indices.map(f => (f, contents(f)))
              .filterNot(f => f._2 == null || f._2.getType == Material.AIR)
              .foreach(f => {
                ps.setInt(2, f._1)
                ps.setBytes(3, f._2.serializeAsBytes())
                ps.addBatch()
              })
            ps.executeBatch()
            new BukkitRunnable {
              override def run(): Unit = {
                callback.success()
              }
            }.runTask(plugin)
          } catch {
            case e: SQLException =>
              new BukkitRunnable {
                override def run(): Unit = {
                  callback.failure(e)
                }
              }.runTask(plugin)
          } finally {
            s.close()
            ps.close()
            c.close()
          }
        }
      }.runTaskAsynchronously(plugin)
    }
  }

  /**
   * 試合中に切断した場合(Gameインスタンスが設定している場合)にtrueにする
   *
   * @version v1.3.22
   * @param uuid 対象のUUID
   */
  def setDisconnect(uuid: String, disconnect: Boolean): Unit = {
    new BukkitRunnable {
      override def run(): Unit = {
        val c = hikari.getConnection
        val ps = c.prepareStatement("UPDATE player SET disconnect=? WHERE uuid=?")
        try {
          ps.setBoolean(1, disconnect)
          ps.setString(2, uuid)
          ps.executeUpdate()
        } catch {
          case e: SQLException =>
            e.printStackTrace()
        } finally {
          ps.close()
          c.close()
        }
      }
    }.runTaskAsynchronously(plugin)
  }

  /**
   * データベースから未加工のデータを取得する
   *
   * @param uuid   UUID
   * @param offset 取得を始める番号
   * @return (type, name, amount, use)の組
   */
  override def getOriginalItem(uuid: String, offset: Int): List[WeaponDB.OriginalItemSet] = {
    var list = List.empty[WeaponDB.OriginalItemSet]
    Using.Manager { use =>
      val c = use(hikari.getConnection)
      val s = use(c.createStatement())
      val rs = s.executeQuery(s"SELECT type, name, amount, use FROM weapon WHERE uuid='$uuid' LIMIT 45 OFFSET $offset")
      while (rs.next()) {
        list :+= new WeaponDB.OriginalItemSet(
          `type` = rs.getString("type"),
          name = rs.getString("name"),
          amount = rs.getInt("amount"),
          use = rs.getBoolean("use")
        )
      }
      rs.close()
      list
    }.getOrElse(list)
  }

  /**
   * 旧getOriginalItemの互換性対応版
   * @param uuid
   * @param offset
   * @return
   */
  @Deprecated
  override def getOriginalItemLegacy(uuid: String, offset: Int): List[(String, String, Int, Boolean)] = {
    var list = List.empty[(String, String, Int, Boolean)]
    Using.Manager { use =>
      val c = use(hikari.getConnection)
      val s = use(c.createStatement())
      val rs = s.executeQuery(s"SELECT type, name, amount, use FROM weapon WHERE uuid='$uuid' LIMIT 45 OFFSET $offset")
      while (rs.next()) {
        list :+= (rs.getString("type"), rs.getString("name"), rs.getInt("amount"), rs.getBoolean("use"))
      }
      rs.close()
      list
    }.getOrElse(list)
  }

  /**
   * データベースから未加工のデータを取得する。この場合はタイプを考える
   *
   * @param uuid   UUID
   * @param offset 取得を始める番号
   * @param `type` アイテムのタイプ
   * @return (name, amount, use)の組
   */
  override def getOriginalItem(uuid: String, offset: Int, `type`: String): List[WeaponDB.OriginalItemSet] = {
    var list = List.empty[WeaponDB.OriginalItemSet]
    Using.Manager { use =>
      val c = use(hikari.getConnection)
      val s = use(c.createStatement())
      val rs = s.executeQuery(s"SELECT name, amount, use FROM weapon WHERE uuid='$uuid' and type='${`type`}' LIMIT 45 OFFSET $offset")
      while (rs.next()) {
        list :+= new WeaponDB.OriginalItemSet(
          `type` = null,
          name = rs.getString("name"),
          amount = rs.getInt("amount"),
          use = rs.getBoolean("use")
        )
      }
      rs.close()
      list
    }.getOrElse(list)
  }

  /**
   * 旧getOriginalItemの互換性版
   * @param uuid
   * @param offset
   * @param weaponType
   * @return
   */
  @Deprecated
  override def getOriginalItemLegacy(uuid: String, offset: Int, `type`: String): List[(String, Int, Boolean)] = {
    var list = List.empty[(String, Int, Boolean)]
    Using.Manager { use =>
      val c = use(hikari.getConnection)
      val s = use(c.createStatement())
      val rs = s.executeQuery(s"SELECT name, amount, use FROM weapon WHERE uuid='$uuid' and type='${`type`}' LIMIT 45 OFFSET $offset")
      while (rs.next()) {
        list :+= (rs.getString("name"), rs.getInt("amount"), rs.getBoolean("use"))
      }
      rs.close()
      list
    }.getOrElse(list)
  }
  /**
   * データベースからアイテムの数字を取得する
   *
   * @param uuid   UUID
   * @param name   名前
   * @param `type` タイプ
   * @return
   */
  override def getAmount(uuid: String, name: String, `type`: String = "item"): Int = {
    val c = hikari.getConnection
    val s = c.createStatement()
    try {
      val rs = s.executeQuery(s"SELECT amount FROM weapon WHERE uuid='$uuid' and type='${`type`}' and name='$name'")
      if (rs.next()) {
        rs.getInt("amount")
      } else {
        0
      }
    } catch {
      case e: SQLException =>
        e.printStackTrace()
        -1
    } finally {
      s.close()
      c.close()
    }
  }


  /**
   * 武器を取得する
   *
   * @param uuid       対象のUUID
   * @param weaponType 武器のタイプ
   * @return 武器たち
   */
  override def getWeapons(uuid: String, weaponType: String, sortType: Int = 0): Seq[Item] = {
    var seq = IndexedSeq.empty[Item]
    Using.Manager { use =>
      val c = use(hikari.getConnection)
      val s = use(c.createStatement())
      val rs = sortType match {
        // 個数でソート
        case 2 =>
          s.executeQuery(s"SELECT weapon.name, amount FROM weapon WHERE uuid='$uuid' and type='$weaponType' ORDER BY amount DESC")
        // アイテムの名前でソート
        case 1 =>
          s.executeQuery(s"SELECT weapon.name, amount FROM weapon INNER JOIN item ON item.name = weapon.name WHERE uuid='$uuid' and type='$weaponType' ORDER BY item.displayname")
        // 何もせず(獲得順)
        case _ =>
          s.executeQuery(s"SELECT weapon.name, amount FROM weapon WHERE uuid='$uuid' and type='$weaponType'")
      }
      while (rs.next()) {
        seq :+= new Item(rs.getString(1), rs.getInt(2))
      }
      seq
    }.getOrElse(seq)
  }

  import scala.jdk.CollectionConverters
  override def getWeapons4J(uuid: String, weaponType: String, sortType: Int = 0): java.util.List[Item] = {
    getWeapons(uuid, weaponType, sortType).asJavaCollection.stream().toList
  }

  /**
   * 現在設定されている武器のリストを取得する
   *
   * @param uuid 対象のUUID
   * @return 武器のタプル(メイン, サブ, 近接, アイテム)
   */
  override def getActiveWeapon(uuid: String): WeaponDB.ActiveWeaponSet = {
    Using.Manager { use =>
      val c = use(hikari.getConnection)
      val ps = use(c.prepareStatement("SELECT name FROM weapon WHERE use=? and uuid=? and type=?"))
      ps.setInt(1, 1)
      ps.setString(2, uuid)
      val gr = ((rs: ResultSet) => if (rs.next()) rs.getString(1) else "")
      ps.setString(3, WeaponDB.PRIMARY)
      val primary = gr(ps.executeQuery())
      ps.setString(3, WeaponDB.SECONDARY)
      val secondary = gr(ps.executeQuery())
      ps.setString(3, WeaponDB.MELEE)
      val melee = gr(ps.executeQuery())
      ps.setString(3, WeaponDB.GRENADE)
      val grenade = gr(ps.executeQuery())
      ps.setString(3, WeaponDB.HEAD)
      val head = gr(ps.executeQuery())
      // (primary, secondary, melee, grenade, head)
      new WeaponDB.ActiveWeaponSet(primary, secondary, melee, grenade, head)
    }.getOrElse(new WeaponDB.ActiveWeaponSet("", "", "", "", ""))
  }


  /**
   * 武器をセットする
   *
   * @param uuid       対象のUUID
   * @param weaponType 武器のタイプ
   * @param name       武器のデータ
   */
  override def setWeapon(uuid: String, weaponType: String, name: String): Unit = {
    Using.Manager { use =>
      val c = use(hikari.getConnection)
      val s = use(c.createStatement())
      s.addBatch(s"UPDATE weapon SET use=0 WHERE uuid='$uuid' and type='$weaponType'")
      s.addBatch(s"UPDATE weapon SET use=1 WHERE uuid='$uuid' and type='$weaponType' and name='$name'")
      s.executeBatch()
    }
  }

  /**
   * 武器を追加する
   *
   * @param uuid       対象のUUID
   * @param weaponType 武器のタイプ
   * @param name       武器のデータ
   */
  override def addWeapon(uuid: String, weaponType: String, name: String, amount: Int = 1): Try[Unit] = {
    Using.Manager { use =>
      val c = use(hikari.getConnection)
      val s = use(c.createStatement())
      s.executeUpdate(s"INSERT INTO weapon (uuid, type, name, amount) VALUES('$uuid', '$weaponType', '$name', $amount) ON CONFLICT(uuid, type, name) DO UPDATE SET amount = amount+$amount")
    }
  }

  override def addWeapon4J(uuid: String, weaponType: String, name: String, amount: Int = 1): Boolean = {
    addWeapon(uuid, weaponType, name, amount) match {
      case Success(_) =>
        true
      case Failure(exception) =>
        exception.printStackTrace()
        false
    }
  }

  /**
   * アイテムを追加する。タイプはitemに固定される。さらに非同期！
   *
   * @param uuid 対象のUUID
   * @param item アイテム
   */
  override def addItem(uuid: String, item: Array[Item]): Unit = {
    new BukkitRunnable {
      override def run(): Unit = {
        Using.Manager { use =>
          val c = use(hikari.getConnection)
          val s = use(c.prepareStatement("INSERT INTO weapon (uuid, type, name, amount) VALUES(?, ?, ?, ?) ON CONFLICT(uuid, type, name) DO UPDATE SET amount = amount+?"))
          s.setString(1, uuid)
          s.setString(2, WeaponDB.ITEM)
          item.foreach(i => {
            s.setString(3, i.name)
            s.setInt(4, i.amount)
            s.setInt(5, i.amount)
            s.addBatch()
          })
          s.executeBatch()
        }
      }
    }.runTaskAsynchronously(plugin)
  }

  def buylWeapon(uuid: String, weaponType: String, shop: ShopUtil.Shop, callback: Callback[String]): Unit = {
    Using.Manager { use =>
      val c = use(hikari.getConnection)
      val s = use(c.createStatement())
      val text = shop.price.map(shopItem => s"(name='${shopItem.name}' AND amount>=${shopItem.amount})").mkString(" OR ")
      val rs = s.executeQuery(s"SELECT CASE WHEN $text THEN 1 ELSE 0 FROM weapon WHERE uuid='$uuid'")
      if (rs.next() && rs.getBoolean(1)) {
        addWeapon(uuid, shop.`type`, shop.product.name, shop.product.amount)
        delWeapon(uuid, shop.price)
      }
    }
  }

  /**
   * 武器を削除する
   *
   * @param uuid
   * @param price
   */
  override def delWeapon(uuid: String, price: Array[Item]): Unit = {
    Using.Manager { use =>
      val c = use(hikari.getConnection)
      val s = use(c.createStatement())
      price.foreach(item => {
        s.addBatch(s"UPDATE weapon SET amount=amount-${item.amount} WHERE uuid='$uuid' and name='${item.name}'")
      })
      s.addBatch(s"DELETE FROM weapon WHERE uuid='$uuid' and 0>=amount")
      s.executeBatch()
    }
  }

  /**
   * 実際にプレイやーが所持しているアイテムを付け加えて返す。非同期で使う
   *
   * @param uuid UUID
   * @param item Shop.priceで獲得できる
   * @return Itemと実際に所持しているアイテムの組
   */
  override def getRequireItemsAmount(uuid: String, item: Array[Item]): Map[String, Int] = {
    var map = Map.empty[String, Int]
    Using.Manager { use =>
      val c = use(hikari.getConnection)
      val s = use(c.createStatement())
      val rs = s.executeQuery(s"SELECT name, amount FROM weapon WHERE uuid='$uuid' and name in (${item.map(f => s"'${f.name}'").mkString(", ")})")
      while (rs.next()) {
        map += rs.getString("name") -> rs.getInt("amount")
      }
      map
    }.getOrElse(map)
  }

  /**
   * item.ymlからデータベースへ移動する
   */
  override def migrate(): Try[Unit] = {
    Using.Manager { use =>
      val c = use(hikari.getConnection())
      val ps = use(c.prepareStatement("INSERT INTO item VALUES(?, ?, ?)"))
      ItemUtil.cache.foreach(f => {
        ps.setString(1, f._1)
        ps.setString(2, ChatColor.stripColor(WarsCoreAPI.getItemStackName(f._2)))
        ps.setBytes(3, f._2.serializeAsBytes())
        ps.addBatch()
        plugin.getLogger.info(s"added batch ${f._1}")
      })
      ps.executeBatch()
    }
  }

  /**
   * データベースにアイテムを登録する
   *
   * @param name アイテムのID
   * @param item アイテム, Noneなら削除を意味する
   */
  override def updateItem(name: String, item: Option[ItemStack]): Try[Unit] = {
    Using.Manager { use =>
      val c = use(hikari.getConnection)
      item match {
        case Some(itemStack) =>
          val ps = use(c.prepareStatement("REPLACE INTO item VALUES(?, ?, ?)"))
          val displayname = ChatColor.stripColor(WarsCoreAPI.getItemStackName(itemStack))
          ps.setString(1, name)
          ps.setString(2, displayname)
          ps.setBytes(3, itemStack.serializeAsBytes())
          ps.executeUpdate()
        case None =>
          val ps = use(c.prepareStatement("DELETE FROM item WHERE name=?"))
          ps.setString(1, name)
          ps.executeUpdate()
      }
    }
  }

  /**
   * データベースのカラムをすべて持ってくる
   *
   * @return
   */
  override def getItems: Try[Seq[(String, String, ItemStack)]] = {
    Using.Manager { use =>
      val c = use(hikari.getConnection)
      val s = use(c.createStatement())
      var seq = Seq.empty[(String, String, ItemStack)]
      val rs = use(s.executeQuery("SELECT * FROM item"))
      while (rs.next()) {
        seq :+= (rs.getString("name"), rs.getString("displayname"), ItemStack.deserializeBytes(rs.getBytes("data")))
      }
      seq
    }
  }

  /**
   * すべてのタグを取得する
   *
   * @return
   */
  override def getTags: Try[Seq[(String, String)]] = {
    Using.Manager { use =>
      val c = use(hikari.getConnection)
      val s = use(c.createStatement())
      var seq = Seq.empty[(String, String)]
      val rs = use(s.executeQuery("SELECT * FROM tag"))
      while (rs.next()) {
        seq :+= (rs.getString("id"), rs.getString("title"))
      }
      seq
    }
  }
}
