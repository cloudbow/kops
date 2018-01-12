/*
 * MongoDao.scala
 * @author arung
 **********************************************************************

             Copyright (c) 2004 - 2018 by Sling Media, Inc.

All rights are reserved.  Reproduction in whole or in part is prohibited
without the written consent of the copyright owner.

Sling Media, Inc. reserves the right to make changes without notice at any time.

Sling Media, Inc. makes no warranty, expressed, implied or statutory, including
but not limited to any implied warranty of merchantability of fitness for any
particular purpose, or that the use will not infringe any third party patent,
copyright or trademark.

Sling Media, Inc. must not be liable for any loss or damage arising from its
use.

This Copyright notice may not be removed or modified without prior
written consent of Sling Media, Inc.

 ***********************************************************************/
package com.slingmedia.sportscloud.dao
import org.mongodb.scala._
import org.mongodb.scala.model.Aggregates._
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.model.Projections._
import org.mongodb.scala.model.Sorts._
import org.mongodb.scala.model.Updates._
import org.mongodb.scala.model._


import scala.concurrent.duration._
import scala.concurrent._


import com.google.gson.{ JsonElement , JsonArray , JsonParser }


import org.mongodb.scala.bson.ObjectId
import org.mongodb.scala.ObservableImplicits

/**
 * Performs CRUD operations for Mongo DB
 * 
 * @author arung
 * @version 1.0
 * @since 1.0
 */
object MongoDao {

  private var mongoClient: MongoClient = null
  private var database: MongoDatabase = null
  
/**
  * The main method for testing Mongo DB operations
  *
  * @param url the external URL
  * @return the response in JSON string format
  */  	
  def main(args: Array[String]) {
     init("mongodb://sportapi2:Eo8ahsiera@cqhlsdb02.sling.com:2701/eventstorage")
     getDataById("testId1","test_collection")       
  }

 /**
  * Initializes Mongo DB client
  */
  def init(mongoURL: String) {
    // Use a Connection String
    mongoClient = MongoClient(mongoURL)
    database = mongoClient.getDatabase("eventstorage")

  }

  /**
  * Fetches the data for given Collection of documents and filters data for given id
  *
  * @param id the doc id
  * @param collectionName the mongo collection name
  * @return the result in JSON format
  */
  def getDataById(id: String, collectionName: String): JsonElement = {

    val collection: MongoCollection[Document] = database.getCollection(collectionName);
    val data = collection.find(equal("id", id))
    val jsonParser = new JsonParser()
    var finalObj:JsonElement = jsonParser.parse("{}")
    if (data != null) {
      val future = data.collect().toFuture()
      val mongoDocs = Await.result(future, 120 seconds)
      if(mongoDocs!=null)
      {    
        val mongoDoc = mongoDocs.head.toJson
        println ("Mongo Doc="+mongoDoc)
        finalObj = jsonParser.parse(mongoDoc)
      }     
    }
    finalObj
  }


  

}