package com.slingmedia.sportscloud.parsers.model

case class League(name:String,fullName:String)

object LeagueEnum {

  val MLB = League("MLB","baseball")
  val NFL = League("NFL", "National Football League")
  val NCAAF= League("NCAAF","College Football League")
  val NBA = League("NBA","National Basketball Association")
  val NCAAB = League("NCAAB", "College Basketball")
  val NHL = League("NHL", "National Basketball League")
  val SOCCER = League("SOCCER", "American Football")

}

