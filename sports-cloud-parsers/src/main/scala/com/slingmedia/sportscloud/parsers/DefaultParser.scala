package com.slingmedia.sportscloud.parsers

import scala.xml.Elem
import org.apache.kafka.connect.source.SourceRecord
import org.apache.kafka.connect.data.Schema
import org.apache.kafka.connect.data.SchemaBuilder
import org.apache.kafka.connect.data.Struct
import scala.collection.JavaConverters._
import com.slingmedia.sportscloud.parsers.factory.ParsedItem


class DefaultParser extends ParsedItem {
  def generateRows(data: Elem, in: SourceRecord): java.util.List[SourceRecord] = {
    val version = (data \\ "version" \ "@number").toString
    val message = GenericData(version)
    (new SourceRecord(in.sourcePartition, in.sourceOffset, in.topic, 0, in.keySchema, in.key, message.connectSchema, message.getStructure) :: List()).asJava
  }

  case class GenericData(version: String) {

    val versionSchema: Schema = SchemaBuilder.struct().name("c.s.s.s.Version").field("version", Schema.STRING_SCHEMA).build()
    val connectSchema: Schema = versionSchema
    val versionStruct: Struct = new Struct(versionSchema).put("version", version)
    def getStructure: Struct = versionStruct

  }
}