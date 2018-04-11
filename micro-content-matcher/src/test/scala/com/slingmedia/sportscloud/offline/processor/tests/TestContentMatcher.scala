package com.slingmedia.sportscloud.offline.processor.tests

import com.slingmedia.sportscloud.offline.batch.impl.ContentMatcher
import org.scalatest._

class TestContentMatcher extends FlatSpec {

  val testContentMatcherTarget = new ContentMatcher()

  //1.
  //homeTeamName | homeTeamCity | awayTeamName | awayTeamCity
  //Raptors     |Toronto     |Trail Blazers|Portland
  "A content match dataframe with homeTeamName and awayTeamName" should
    "generate regexp ^.*?\\s*Raptors\\s*(vs\\.|at)\\s*Trail Blazers.*$" in {
    val regexp = testContentMatcherTarget.lookupAndCreateColumn(
      List("homeTeamName", "awayTeamName"),
      "not_defined", "not_defined", "Trail Blazers", "Raptors")
    assert(regexp == "^.*?\\s*Raptors\\s*(vs\\.|at)\\s*Trail Blazers.*$")
  }


  //2.
  //homeTeamName | homeTeamCity | awayTeamName | awayTeamCity
  //Raptors     |Toronto     |Trail Blazers|Portland
  "A content match dataframe with homeTeamName , awayTeamCity and awayTeamName" should
    "generate regexp ^.*?\\s*Raptors\\s*(vs\\.|at)\\s*Portland.*Trail Blazers.*$" in {
    val regexp = testContentMatcherTarget.lookupAndCreateColumn(
      List("homeTeamName", "awayTeamCity","awayTeamName"),
      "not_defined", "Trail Blazers", "Portland", "Raptors")
    assert(regexp == "^.*?\\s*Raptors\\s*(vs\\.|at)\\s*Portland.*Trail Blazers.*$")
  }

  //3.
  //homeTeamName | homeTeamCity | awayTeamName | awayTeamCity
  //Raptors     |Toronto     |Trail Blazers|Portland
  "A content match dataframe with homeTeamName , homeTeamCity , awayTeamName,awayTeamCity" should
    "generate regexp ^.*?\\s*Raptors.*Toronto\\s*(vs\\.|at)\\s*Trail Blazers.*Portland.*$" in {
    val regexp = testContentMatcherTarget.lookupAndCreateColumn(
      List("homeTeamName", "homeTeamCity", "awayTeamName","awayTeamCity"),
      "Portland", "Trail Blazers", "Toronto", "Raptors")
    assert(regexp == "^.*?\\s*Raptors.*Toronto\\s*(vs\\.|at)\\s*Trail Blazers.*Portland.*$")
  }

  //4.
  //homeTeamName | homeTeamCity | awayTeamName | awayTeamCity
  //Raptors     |Toronto     |Trail Blazers|Portland

  "A content match dataframe with homeTeamCity , homeTeamName , awayTeamCity,awayTeamName" should
    "generate regexp ^.*?\\s*Toronto.*Raptors\\s*(vs\\.|at)\\s*Portland.*Trail Blazers.*$" in {
    val regexp = testContentMatcherTarget.lookupAndCreateColumn(
      List("homeTeamCity", "homeTeamName", "awayTeamCity","awayTeamName"),
      "Trail Blazers", "Portland", "Raptors", "Toronto")
    assert(regexp == "^.*?\\s*Toronto.*Raptors\\s*(vs\\.|at)\\s*Portland.*Trail Blazers.*$")
  }

  //5.
  //homeTeamName | homeTeamCity | awayTeamName | awayTeamCity
  //Raptors     |Toronto     |Trail Blazers|Portland
  "A content match dataframe with homeTeamCity , homeTeamName , awayTeamCity,awayTeamName" should
    "generate regexp ^.*?\\s*Toronto.*Raptors\\s*(vs\\.|at)\\s*Trail Blazers.*$" in {
    val regexp = testContentMatcherTarget.lookupAndCreateColumn(
      List("homeTeamCity","homeTeamName","awayTeamName"),"not_defined",
    "Trail Blazers", "Raptors", "Toronto")
    assert(regexp == "^.*?\\s*Toronto.*Raptors\\s*(vs\\.|at)\\s*Trail Blazers.*$")
  }

  //6.
  //homeTeamName | homeTeamCity | awayTeamName | awayTeamCity
  //Raptors     |Toronto     |Trail Blazers|Portland
  "A content match dataframe with homeTeamCity , homeTeamName , awayTeamCity,awayTeamName" should
    "generate regexp ^.*?\\s*Toronto\\s*(vs\\.|at)\\s*Portland.*$" in {
    val regexp = testContentMatcherTarget.lookupAndCreateColumn(
      List("homeTeamCity", "awayTeamCity"),"not_defined",
      "not_defined", "Portland", "Toronto")
    assert(regexp == "^.*?\\s*Toronto\\s*(vs\\.|at)\\s*Portland.*$")
  }

  //7.
  //homeTeamName | homeTeamCity | awayTeamName | awayTeamCity
  //Raptors     |Toronto     |Trail Blazers|Portland
  "A content match dataframe with homeTeamCity , homeTeamName , awayTeamCity,awayTeamName" should
    "generate regexp ^.*?\\s*Trail Blazers\\s*(vs\\.|at)\\s*Raptors.*$" in {
    val regexp = testContentMatcherTarget.lookupAndCreateColumn(
      List("awayTeamName", "homeTeamName"),"not_defined",
      "not_defined", "Raptors", "Trail Blazers")
    assert(regexp == "^.*?\\s*Trail Blazers\\s*(vs\\.|at)\\s*Raptors.*$")
  }

  //8.
  //homeTeamName | homeTeamCity | awayTeamName | awayTeamCity
  //Raptors     |Toronto     |Trail Blazers|Portland
  "A content match dataframe with homeTeamCity , homeTeamName , awayTeamCity,awayTeamName" should
    "generate regexp ^.*?\\s*Trail Blazers\\s*(vs\\.|at)\\s*Toronto.*Raptors.*$" in {
    val regexp = testContentMatcherTarget.lookupAndCreateColumn(
      List("awayTeamName","homeTeamCity","homeTeamName"),
      "not_defined",
      "Raptors",
      "Toronto",
      "Trail Blazers")
    assert(regexp == "^.*?\\s*Trail Blazers\\s*(vs\\.|at)\\s*Toronto.*Raptors.*$")
  }

  //9.
  //homeTeamName | homeTeamCity | awayTeamName | awayTeamCity
  //Raptors     |Toronto     |Trail Blazers|Portland
  "A content match dataframe with homeTeamCity , homeTeamName , awayTeamCity,awayTeamName" should
    "generate regexp ^.*?\\s*Trail Blazers.*Portland\\s*(vs\\.|at)\\s*Raptors.*Toronto.*$" in {
    val regexp = testContentMatcherTarget.lookupAndCreateColumn(
      List("awayTeamName","awayTeamCity","homeTeamName","homeTeamCity"),
      "Toronto",
      "Raptors",
      "Portland",
      "Trail Blazers")
    assert(regexp == "^.*?\\s*Trail Blazers.*Portland\\s*(vs\\.|at)\\s*Raptors.*Toronto.*$")
  }

  //10.
  //homeTeamName | homeTeamCity | awayTeamName | awayTeamCity
  //Raptors     |Toronto     |Trail Blazers|Portland
  "A content match dataframe with homeTeamCity , homeTeamName , awayTeamCity,awayTeamName" should
    "generate regexp ^.*?\\s*Portland.*Trail Blazers\\s*(vs\\.|at)\\s*Raptors.*$" in {
    val regexp = testContentMatcherTarget.lookupAndCreateColumn(
      List("awayTeamCity","awayTeamName","homeTeamName"),
      "not_defined",
      "Raptors",
      "Trail Blazers",
      "Portland")
    assert(regexp == "^.*?\\s*Portland.*Trail Blazers\\s*(vs\\.|at)\\s*Raptors.*$")
  }

  //11.
  //homeTeamName | homeTeamCity | awayTeamName | awayTeamCity
  //Raptors     |Toronto     |Trail Blazers|Portland
  "A content match dataframe with homeTeamCity , homeTeamName , awayTeamCity,awayTeamName" should
    "generate regexp ^.*?\\s*Portland.*Trail Blazers\\s*(vs\\.|at)\\s*Toronto.*Raptors.*$" in {
    val regexp = testContentMatcherTarget.lookupAndCreateColumn(
      List("awayTeamCity","awayTeamName","homeTeamCity","homeTeamName"),
      "Raptors",
      "Toronto",
      "Trail Blazers",
      "Portland")
    assert(regexp == "^.*?\\s*Portland.*Trail Blazers\\s*(vs\\.|at)\\s*Toronto.*Raptors.*$")
  }

  //12.
  //homeTeamName | homeTeamCity | awayTeamName | awayTeamCity
  //Raptors     |Toronto     |Trail Blazers|Portland
  "A content match dataframe with homeTeamCity , homeTeamName , awayTeamCity,awayTeamName" should
    "generate regexp ^.*?\\s*Portland\\s*(vs\\.|at)\\s*Toronto.*$" in {
    val regexp = testContentMatcherTarget.lookupAndCreateColumn(
      List("awayTeamCity","homeTeamCity"),
      "not_defined",
      "not_defined",
      "Toronto",
      "Portland")
    assert(regexp == "^.*?\\s*Portland\\s*(vs\\.|at)\\s*Toronto.*$")
  }


  //13.
  //homeTeamName | homeTeamCity | awayTeamName | awayTeamCity
  //Raptors     |Toronto     |Trail Blazers|Portland
  "A content match dataframe with homeTeamName and empty awayTeamName" should
    "generate regexp (?!.*)" in {
    val regexp = testContentMatcherTarget.lookupAndCreateColumn(
      List("homeTeamName", "awayTeamName"),
      "not_defined", "not_defined", " ", "Raptors")
    assert(regexp == "(?!.*)")
  }



}
