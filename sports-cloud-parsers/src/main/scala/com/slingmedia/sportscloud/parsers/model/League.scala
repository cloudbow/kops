package com.slingmedia.sportscloud.parsers.model

case class League(name:String,fullName:String)

object LeagueEnum {

  val MLB = League("MLB","baseball")
  val NFL = League("NFL", "National Football League")
  val NCAAF= League("NCAAF","College Football League")
}

