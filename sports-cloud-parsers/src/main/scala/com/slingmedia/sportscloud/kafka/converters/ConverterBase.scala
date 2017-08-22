package com.slingmedia.sportscloud.kafka.converters

import scala.xml.Elem
import scala.util.{Try}

trait ConverterBase {

  def loadXML(line: String): Try[Elem] = {    
      Try(scala.xml.XML.loadString(line))
  }
  
}