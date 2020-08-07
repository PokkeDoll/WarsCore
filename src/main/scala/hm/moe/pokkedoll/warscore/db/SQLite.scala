package hm.moe.pokkedoll.warscore.db

import java.sql.SQLException
import java.util.UUID

import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import hm.moe.pokkedoll.warscore.{WPlayer, WarsCore}
import org.bukkit.entity.Item
import org.bukkit.inventory.ItemStack

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
          | name TEXT
          |);
          |
          |CREATE TABLE IF NOT EXISTS rank (
          | uuid TEXT PRIMARY KEY,
          | rank INTEGER DEFAULT 1,
          | exp INTEGER DEFAULT 0,
          | FOREIGN KEY(uuid) REFERENCES player(uuid)
          | ON DELETE CASCADE ON UPDATE CASCADE
          |);
          |
          |CREATE TABLE IF NOT EXISTS tag (
          | uuid TEXT PRIMARY KEY,
          | id TEXT,
          | storage TEXT,
          | FOREIGN KEY(uuid) REFERENCES player(uuid)
          | ON DELETE CASCADE ON UPDATE CASCADE
          |);
          |
          |CREATE TABLE IF NOT EXISTS donate (
          | uuid TEXT PRIMARY KEY,
          | ID INTEGER DEFAULT 0
          | killSoundID INTEGER DEFAULT 0,
          | killEffectID INTEGER DEFAULT 0,
          | FOREIGN KEY(uuid) REFERENCES player(uuid)
          | ON DELETE CASCADE ON UPDATE CASCADE
          |);
          |
          |CREATE TABLE IF NOT EXISTS donate (
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
          |""".stripMargin)
    } catch {
      case e: SQLException => e.printStackTrace()
    }
  }

  /**
   * UUID(=データ、つまりテーブル)があるか確認するメソッド
   *
   * @param uuid
   * @return データがあるならtrueを返す
   */
  override def hasUUID(uuid: UUID): Boolean = ???

  /**
   * テーブルからデータを読み込むメソッド
   *
   * @param wp
   * @return 読み込みエラーが発生したらNone
   */
  override def loadWPlayer(wp: WPlayer): Option[WPlayer] = ???

  /**
   * テーブルにデータを保存するメソッド
   */
  override def saveWPlayer(wp: WPlayer): Option[WPlayer] = ???

  /**
   * データを登録するメソッド
   *
   * @param uuid
   * @return
   */
  override def insert(uuid: UUID): Boolean = ???

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

  override def getStorage(id: Int, uuid: String): Option[Array[Byte]] = {
    val c = hikari.getConnection
    val ps = c.prepareStatement(s"SELECT s${id} FROM storage WHERE uuid=?")
    try {
      ps.setString(1, uuid)
      val rs = ps.executeQuery()
      if(rs.next()) {
        Option(rs.getBytes(0))
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
}
