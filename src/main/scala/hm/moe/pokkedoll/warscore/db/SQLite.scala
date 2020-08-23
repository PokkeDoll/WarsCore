package hm.moe.pokkedoll.warscore.db

import java.sql.SQLException
import java.util.UUID

import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import hm.moe.pokkedoll.warscore.{WPlayer, WarsCore}
import org.bukkit.entity.Item
import org.bukkit.inventory.ItemStack

import scala.util.Try

class SQLite(plugin: WarsCore) extends Database {

  private val path = "database.db"
  private val config = new HikariConfig()

  config.setDriverClassName("org.sqlite.JDBC")
  config.setJdbcUrl(s"jdbc:sqlite:${plugin.getDataFolder.getAbsolutePath}/$path")
  config.setConnectionInitSql("SELECT 1")

  val hikari = new HikariDataSource(config)

  /**
   * インスタンス作成時に呼び出されるメソッド
   */
  def init(): Unit = {
    val c = hikari.getConnection()
    try {
      val st = c.createStatement()
      /* 基本情報これさえあれば困らないデータ */
      // TDM... play, win, kill, death, assist, damage: Int = _

      st.execute(
        """
          |CREATE TABLE IF NOT EXISTS player (
          | uuid TEXT PRIMARY KEY,
          | donateID INTEGER DEFAULT 0
          |);
          |
          |CREATE TABLE IF NOT EXISTS rank (
          | uuid TEXT PRIMARY KEY,
          | id INTEGER DEFAULT 1,
          | exp INTEGER DEFAULT 0,
          | FOREIGN KEY(uuid) REFERENCES player(uuid)
          | ON DELETE CASCADE ON UPDATE CASCADE
          |);
          |
          |CREATE TABLE IF NOT EXISTS tag (
          | uuid TEXT PRIMARY KEY,
          | id TEXT DEFAULT "",
          | storage TEXT DEFAULT "",
          | FOREIGN KEY(uuid) REFERENCES player(uuid)
          | ON DELETE CASCADE ON UPDATE CASCADE
          |);
          |
          |CREATE TABLE IF NOT EXISTS enderchest (
          | uuid TEXT PRIMARY KEY,
          | s0 BLOB,
          | s1 BLOB,
          | s2 BLOB,
          | s3 BLOB,
          | s4 BLOB,
          | s5 BLOB,
          | s6 BLOB,
          | s7 BLOB,
          | s8 BLOB,
          | s9 BLOB,
          | s10 BLOB,
          | s11 BLOB,
          | s12 BLOB,
          | s13 BLOB,
          | s14 BLOB,
          | s15 BLOB,
          | s16 BLOB,
          | s17 BLOB,
          | s18 BLOB,
          | s19 BLOB,
          | s20 BLOB,
          | s21 BLOB,
          | s22 BLOB,
          | s23 BLOB,
          | s24 BLOB,
          | s25 BLOB,
          | s26 BLOB,
          | FOREIGN KEY(uuid) REFERENCES player(uuid)
          | ON DELETE CASCADE ON UPDATE CASCADE
          |);
          |
          |CREATE TABLE IF NOT EXISTS tdm (
          | uuid TEXT,
          | play INTEGER DEFAULT 0,
          | win INTEGER DEFAULT 0,
          | kill INTEGER DEFAULT 0,
          | death INTEGER DEFAULT 0,
          | assist INTEGER DEFAULT 0,
          | damage INTEGER DEFAULT 0
          |);
          |
          |CREATE TABLE IF NOT EXISTS tactics (
          | uuid TEXT,
          | play INTEGER DEFAULT 0,
          | win INTEGER DEFAULT 0,
          | FOREIGN KEY(uuid) REFERENCES player(uuid)
          | ON DELETE CASCADE ON UPDATE CASCADE
          |);
          |""".stripMargin)
    } catch {
      case e: SQLException => e.printStackTrace()
    }
  }

  //init()

  /**
   * UUID(=データ、つまりテーブル)があるか確認するメソッド
   *
   * @param uuid
   * @return データがあるならtrueを返す
   */
  override def hasUUID(uuid: UUID): Boolean = {
    val c = hikari.getConnection()
    val ps = c.prepareStatement("SELECT uuid FROM player WHERE uuid=?")
    try {
      ps.setString(1, uuid.toString)
      val rs = ps.executeQuery()
      rs.next()
    } catch {
      case e: SQLException =>
        e.printStackTrace()
        false
    } finally {
      ps.close()
      c.close()
    }
  }

  /**
   * テーブルからデータを読み込むメソッド
   *
   * @param wp
   * @return 読み込みエラーが発生したらNone
   */
  override def loadWPlayer(wp: WPlayer): Option[WPlayer] = None

  /**
   * テーブルにデータを保存するメソッド
   */
  override def saveWPlayer(wp: WPlayer): Option[WPlayer] = None

  /**
   * データを登録するメソッド
   *
   * @param uuid
   * @return
   */
  override def insert(uuid: UUID): Boolean = {
    val c = hikari.getConnection()
    val s = c.createStatement()
    try {
      val build = (table: String) => s"INSERT INTO $table(`uuid`) VALUES('${uuid.toString}')"
      s.executeUpdate(build("player"))
      s.executeUpdate(build("rank"))
      s.executeUpdate(build("enderchest"))
      s.executeUpdate(build("tdm"))
      s.executeUpdate(build("tactics"))
      true
    } catch {
      case e: SQLException =>
        e.printStackTrace()
        false
    } finally {
      s.close()
      c.close()
    }
  }

  override def getInt(table: String, column: String, uuid: String): Option[Int] = {
    val c = hikari.getConnection
    val ps = c.prepareStatement(s"SELECT $column FROM $table WHERE uuid=?")
    try {
      ps.setString(1, uuid)
      val rs = ps.executeQuery()
      if(rs.next()) {
        Option(rs.getInt(column))
      } else {
        None
      }
    } catch {
      case e: SQLException =>
        e.printStackTrace()
        None
    } finally {
      ps.close()
      c.close()
    }
  }

  override def getStorage(id: Int, uuid: String): Option[String] = {
    val col = s"s$id"
    val c = hikari.getConnection
    val ps = c.prepareStatement(s"SELECT $col FROM `enderchest` WHERE uuid=?")
    try {
      ps.setString(1, uuid)
      val rs = ps.executeQuery()
      if(rs.next()) {
        Option(rs.getString(col))
      } else {
        None
      }
    } catch {
      case e: SQLException =>
        e.printStackTrace()
        None
    } finally {
      ps.close()
      c.close()
    }
  }

  override def setStorage(id: Int, uuid: String, item: String): Unit = {
    val c = hikari.getConnection()
    val ps = c.prepareStatement(s"UPDATE `enderchest` SET `s$id`=? WHERE `uuid`=?")
    try {
      ps.setString(1, item)
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

  override def getRankData(uuid: String): Option[(Int, Int)] = {
    val c = hikari.getConnection()
    val ps = c.prepareStatement("SELECT id, exp FROM rank WHERE uuid=?")
    try {
      ps.setString(1, uuid)
      val rs = ps.executeQuery()
      if(rs.next()) {
        Some(rs.getInt("id"), rs.getInt("exp"))
      } else {
        None
      }
    } catch {
      case e: SQLException =>
        e.printStackTrace()
        None
    } finally {
      ps.close()
      c.close()
    }
  }

  override def updateTDM(): Boolean = ???

  override def close(): Unit = hikari.close()
}
