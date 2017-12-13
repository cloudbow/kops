package com.slingmedia.sportscloud.parsers.model

object League extends Enumeration {
  type League = Value
  val MLB,CFB,NFL,None = Value ;
  def withNameOpt(s: String): Option[Value] = values.find(_.toString == s)
}