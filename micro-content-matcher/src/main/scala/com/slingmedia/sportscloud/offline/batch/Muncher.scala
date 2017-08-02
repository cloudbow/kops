package com.slingmedia.sportscloud.offline.batch

import org.apache.spark.sql.types.StructType
import org.apache.spark.sql.{ DataFrame, Column }
import org.apache.spark.sql.types.{ StructField };
import org.apache.spark.sql.functions.{ col }

trait Muncher {
  def munch(inputKafkaTopic: String, outputCollName: String, zkHost: String): Unit = {}
  def stream(inputKafkaTopic: String, outputCollName: String, zkHost: String): Unit = {}
  def munch(inputKafkaTopic: String, outputCollName: String, zkHost: String, schema: StructType, filterCond: String): Unit = {}
  def munch(batchTime: Long, inputKafkaTopic: String, outputCollName: String, zkHost: String, schema: StructType, imgRequired: Boolean, idColumn: Column, filterCond: String, testColumn: Column): Unit = {}
  val children: (String, DataFrame) => Array[Column] = (colname: String, df: DataFrame) => {
    val parent = df.schema.fields.filter(_.name == colname).head
    val fields = parent.dataType match {
      case x: StructType => x.fields
      case _             => Array.empty[StructField]
    }
    fields.map(x => col(s"$colname.${x.name}"))
  }

}