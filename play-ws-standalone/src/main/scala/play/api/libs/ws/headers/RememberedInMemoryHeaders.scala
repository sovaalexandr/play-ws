package play.api.libs.ws.headers

import play.api.libs.ws.RememberedHeaders

import scala.collection.mutable

class RememberedInMemoryHeaders extends RememberedHeaders {
  private val memory = mutable.Map[String, Map[String, Seq[String]]]()
  override def set(key: String, value: Map[String, Seq[String]]): Unit = memory += (key -> value)

  override def get(key: String): Map[String, Seq[String]] = memory(key)
}
