package com.slingmedia.sportscloud.parsers.factory

import org.apache.kafka.connect.source.SourceRecord
import scala.xml.Elem

trait ParsedItem {
   def generateRows(data:Elem,in:SourceRecord):java.util.List[SourceRecord]
}