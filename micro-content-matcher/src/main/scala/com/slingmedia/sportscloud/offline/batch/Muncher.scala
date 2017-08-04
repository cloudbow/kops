package com.slingmedia.sportscloud.offline.batch

import org.apache.spark.sql.types.StructType
import org.apache.spark.sql.{ DataFrame, Column }
import org.apache.spark.sql.types.{ StructField };
import org.apache.spark.sql.functions.{ col,udf }
import java.time.OffsetDateTime

import com.lucidworks.spark.util.{ SolrSupport, SolrQuerySupport, ConfigurationConstants }


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
  
  val timeStrToEpoch: (String => Long) = (timeStr: String) => {
    if (timeStr == null) 0L else OffsetDateTime.parse(timeStr).toEpochSecond()
  }
  val timeStrToEpochUDF = udf(timeStrToEpoch(_: String))
  
  val getZeroPaddedFunc: (String => String) = (timeStr: String) => {
    val timeInInt = timeStr.toInt
    if (timeInInt < 0) {
      val absTime = Math.abs(timeInInt)
      "-".concat(getZeroPaddedFunc(absTime.toString))
    } else if (timeInInt < 10) {
      "0".concat(timeStr)
    } else {
      timeStr
    }
  }
  val getZeroPaddedUDF = udf(getZeroPaddedFunc(_: String))
  
  val indexToSolr: (String, String, String, DataFrame) => Unit = (zkHost: String, outputCollName: String, generateKey: String, input: DataFrame) => {
    val solrOpts = Map("zkhost" -> zkHost, "collection" -> outputCollName, ConfigurationConstants.GENERATE_UNIQUE_KEY -> generateKey)
    val solrCloudClient = SolrSupport.getCachedCloudClient(zkHost)
    input.write.format("solr").options(solrOpts).mode(org.apache.spark.sql.SaveMode.Overwrite).save()
    solrCloudClient.commit(outputCollName, true, true)
  }

}