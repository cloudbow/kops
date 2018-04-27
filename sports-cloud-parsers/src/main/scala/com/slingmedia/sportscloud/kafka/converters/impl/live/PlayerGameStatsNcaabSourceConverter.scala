package com.slingmedia.sportscloud.kafka.converters.impl.live

import java.util

import com.eneco.trading.kafka.connect.ftp.source.SourceRecordConverter
import com.slingmedia.sportscloud.kafka.converters.ConverterBase
import com.slingmedia.sportscloud.parsers.model.LeagueEnum
import org.apache.kafka.connect.source.SourceRecord
import org.slf4j.LoggerFactory

/**
  * Handler for converting Ncaab Box Score source data to Player stats record
  */
class PlayerGameStatsNcaabSourceConverter extends SourceRecordConverter with ConverterBase {
  private val log = LoggerFactory.getLogger("PlayerGameStatsNbaSourceConverter")

  override def convert(in: SourceRecord): java.util.List[SourceRecord] = {
    log.info("Running PlayerGameStatsNbaSourceConverter")
    generatePlayerGameStatsData(in, LeagueEnum.NCAAB)
  }

  override def configure(props: util.Map[String, _]): Unit = {}
}