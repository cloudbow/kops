package com.slingmedia.sportscloud.common.dao
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

object MongoDao {
  
  
  def main(args: Array[String]) {
     init("mongodb://sportapi2:Eo8ahsiera@cqhlsdb02.sling.com:2701/eventstorage")
     getDataById("testId1","test_collection")       
  }

  private var mongoClient: MongoClient = null
  private var database: MongoDatabase = null

  def init(mongoURL: String) {
    // Use a Connection String
    mongoClient = MongoClient(mongoURL)
    database = mongoClient.getDatabase("eventstorage")

  }

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