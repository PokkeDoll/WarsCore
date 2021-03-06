package hm.moe.pokkedoll.crackshot

import com.shampaggon.crackshot.{CSDirector, CSMinion}
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration

import java.io.File
import scala.util.{Failure, Success, Try}

class WeaponLoader(val csDirector: CSDirector) {
  val csMinion: CSMinion = csDirector.csminion

  var weaponConfig = new YamlConfiguration

  def loadCSWeapons(): Seq[YamlConfiguration] = {
    val tag = new File(csDirector.getDataFolder, "/weapons")
    val fileList = tag.listFiles()
    if(fileList==null) {
      println("[CrackShot] No weapons were loaded!")
      Seq.empty
    } else {
      fileList
        .filter(file => file.isFile && file.getName.endsWith(".yml"))
        .flatMap(loadCSConfig)
        .toSeq
    }
  }

  val loadCSConfig: File => Option[YamlConfiguration] = (file: File) => {
    val config = new YamlConfiguration()
    Try(config.load(file)) match {
      case Success(_) =>
        println("Success to load config!")
        Some(config)
      case Failure(e) =>
        e.printStackTrace()
        println("Fail to load config!")
        None
    }
  }

  def loadWeapons(): Unit = {
    val tag = new File(csDirector.getDataFolder, "/weapons/extended")
    val fileList = tag.listFiles()
    if(fileList == null) {
      csDirector.getLogger.info("[(Extended)CrackShot] No extended weapons were loaded!")
    } else {
      fileList
        .filter(_.getName.endsWith(".yml"))
        .flatMap(loadCSConfig)
        .foreach(config => {
          // CSのファイルをも持ってくる
          val cs = loadCSWeapons()
          // ファイルを合成させる
          val result = new YamlConfiguration
          /*
          テスト
           */
          println("test")
          cs.map(f => f.getKeys(false)).foreach(f => {
            f.forEach(ff => {
              println(ff)
            })
          })
          config.getKeys(false).forEach(key => {
            val extend = config.getString(key + ".extend")
            val data = config.getConfigurationSection(key + ".data")
            if(extend != null && data != null) {
              cs.find(p => p.contains(extend)) match {
                case Some(p) =>
                  weaponConfig = copyConfigurationSection(p.getConfigurationSection(extend), key)
                  println("copied: \n" + weaponConfig.saveToString())

                  weaponConfig = copyConfigurationSection(data, key)

                  println("converted: \n" + weaponConfig.saveToString())

                  csDirector.fillHashMaps(weaponConfig)
                case None =>
                  println(s"$extend not found")
              }
            }
          })
          println("loadadadadadadad!!!")
        })
      csMinion.completeList()
    }
  }


  def copyConfigurationSection(cs: ConfigurationSection, parent: String = ""): YamlConfiguration = {
    cs.getValues(false).forEach((k, v) => {
      if(cs.isConfigurationSection(k)) {
        weaponConfig = copyConfigurationSection(cs.getConfigurationSection(k), if(parent == "") k else parent + "." + k)
      } else {
        weaponConfig.set(if(parent == "") k else parent + "." + k, v)
      }
    })
    weaponConfig
  }
}
