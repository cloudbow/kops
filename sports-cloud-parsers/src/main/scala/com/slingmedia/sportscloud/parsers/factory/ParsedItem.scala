package com.slingmedia.sportscloud.parsers.factory

import org.apache.kafka.connect.source.SourceRecord
import scala.xml.Elem
import scala.xml.NodeSeq
import scala.collection.JavaConverters._

trait ParsedItem {
  def generateRows(data: Elem, in: SourceRecord): java.util.List[SourceRecord] = {
    Array[SourceRecord]().toList.asJava
  }
  
  def generateRows(data: Elem, in: SourceRecord,league:String, sport:String): java.util.List[SourceRecord] = {
    Array[SourceRecord]().toList.asJava
  }
  
  def generateRows(data: Elem, in: SourceRecord, xmlRoot: NodeSeq): java.util.List[SourceRecord] = {
    Array[SourceRecord]().toList.asJava
  }

  def toInt(s: String): Option[Int] = {
    try {
      Some(s.toInt)
    } catch {
      case e: Exception => None
    }
  }
  
  def toFloat(s: String): Option[Float] = {
    try {
      Some(s.toFloat)
    } catch {
      case e: Exception => None
    }
  }
}