package com.slingmedia.sportscloud.rest.tests
import java.util

import collection.mutable.Stack
import org.scalatest._

import sys.process._
import com.slingmedia.sportscloud.netty.rest.server.handler.delegates.SportsCloudHomeScreenDelegate
import com.google.gson.{JsonArray, JsonElement, JsonObject, JsonParser}

import scala.io.Source
import org.mockito.Mockito.{mock, verify, when}
import org.mockito.ArgumentMatchers.any
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer

class TestGameScheduleParsing extends FlatSpec  {

  "When we input elastic json for game schedule" should "parse correctly" in {
    val responseJsonStr =  Source.fromURL(getClass.getResource("/game-schedule.json")).getLines().mkString
    val parser = new JsonParser();
    var responseJson = parser.parse("{}");
    try {
      responseJson = parser.parse(responseJsonStr);
    } catch {
      case e: Exception => println("Error ocurred in parsing")
    }
    val scHomeDelegateMock = mock(classOf[SportsCloudHomeScreenDelegate])
    when(scHomeDelegateMock.prepareLiveGameInfoData(10l,10l,500))
      .thenReturn(new java.util.HashMap[String,com.google.gson.JsonObject]());
    when(scHomeDelegateMock.prepareJsonResponseForHomeScreen("{}", 10l, 10l, new java.util.HashSet[String](), responseJson)).thenCallRealMethod();
    when(scHomeDelegateMock.getMatchedGame(any(),any())).thenAnswer(new MatchedGameAnswer)
    try {
      assert("[]" != scHomeDelegateMock.prepareJsonResponseForHomeScreen("{}", 10l, 10l, new java.util.HashSet[String](), responseJson))
    } catch {
      case e:Exception => fail("Failed to parse json")
    }
  }

  class MatchedGameAnswer extends Answer[JsonObject]{

    override def answer(invocation: InvocationOnMock): JsonObject = {
      val args = invocation.getArguments
      val mock = invocation.getMock
      args.asInstanceOf[Array[Object]](1).asInstanceOf[JsonArray].get(0).getAsJsonObject().get("_source").getAsJsonObject()
    }
  }


}