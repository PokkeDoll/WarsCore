package hm.moe.pokkedoll.crackshot
/**
 * MIT License
 *
 *  Copyright (c) 2021 PokkeDoll
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

import com.shampaggon.crackshot.{CSDirector, CSMinion}
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration

import java.io.File
import scala.util.{Failure, Success, Try}

/**
 * @author Emorard
 * @version 1.0
 * @param csDirector Active CrackShot instance
 */
class WeaponLoader(private val csDirector: CSDirector) {
  private val csMinion: CSMinion = csDirector.csminion

  private var weaponConfig = new YamlConfiguration

  /**
   * Load "CrackShot" file
   */
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

  /**
   * For flatMap
   */
  val loadCSConfig: File => Option[YamlConfiguration] = (file: File) => {
    val config = new YamlConfiguration()
    Try(config.load(file)) match {
      case Success(_) =>
        Some(config)
      case Failure(e) =>
        e.printStackTrace()
        None
    }
  }

  /**
   * Load extended weapon. copy-and-replace
   */
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
          // take cs file
          val cs = loadCSWeapons()
          // mixed file
          config.getKeys(false).forEach(key => {
            val extend = config.getString(key + ".extend")
            val data = config.getConfigurationSection(key + ".data")
            if(extend != null && data != null) {
              cs.find(p => p.contains(extend)) match {
                case Some(p) =>
                  weaponConfig = copyConfigurationSection(p.getConfigurationSection(extend), key)
                  weaponConfig = copyConfigurationSection(data, key)
                  csDirector.fillHashMaps(weaponConfig)
                case None =>
                  println(s"[(Extended)CrackShot] $extend is not found")
              }
            }
          })
        })
      csMinion.completeList()
    }
  }

  /**
   * Copy configuration section
   */
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
