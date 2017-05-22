package com.slingmedia.tests


object VerifyLineupRequests {
  def main(args: Array[String]) {
    if(args.length==0) {
      println ("Arguments not provided.")
      println ("Run as follows")
      println ("Type sbt")
      println ("Type the following on console ")
      println ("> run-main com.slingmedia.tests.VerifyLineupRequests <uuid> <cookie> <rxId> <contentTitle> <callsign> <contentId>")
      println ("<contentTitle> <callsign> should exist together are however optional to <contentId> ")
      println ("Example : run-main com.slingmedia.tests.VerifyLineupRequests ZslMabN5kcrf8rEnnReJivkTGTco6AoY _zeus_f9c3dfa0-d112-4bef-a8e4-a079b33b6aca dish1946221657 \"MLB: Spurs at Rockets\" \"MLB\"")
      println ("Example : run-main com.slingmedia.tests.VerifyLineupRequests ZslMabN5kcrf8rEnnReJivkTGTco6AoY _zeus_f9c3dfa0-d112-4bef-a8e4-a079b33b6aca dish1946221657 \"\" \"\" 2695857")
    }
    val uuid = args(0)
    val cookie = args(1)
    val rxId = args(2)
    var contentTitleArg = None: Option[String]
    var callsignArg = None: Option[String]
    var contentIds = None: Option[List[String]]
    if(args.length>3) {
      contentTitleArg = Some(args(3))
    }
    if(args.length>4) {
      callsignArg = Some(args(4))
    }
    if(args.length>5){
      contentIds = Some(List(args(5)))
    }
    
    args.foreach(println(_))
try {
    val headers = Map("enterprise"-> cookie)
    val rxIdMap = Map("512"-> "1",
                      "522"-> "2",
                      "625"-> "3",
                      "hopper2"-> "11",
                      "hopper3"-> "12",
                      "hopper2000"-> "9",
                      "hpr2000"-> "9",
                      "hprslng"-> "11",
                      "vip612"-> "4",
                      "vip622"-> "5",
                      "vip722"-> "6",
                      "vip722k"-> "7",
                      "vip722s"-> "8",
                      "vip922"-> "8",
                      "vip922s"-> "8")
    val httpClient = buildHttpClient()
    val esraURL = s"http://esra.slingbox.com/receiverinfo/rest/v1/receiverid/$rxId?correlationid=123455"
    val esraResp = get(esraURL, headers, 50000 , 180000, "GET")
    println ("Model id fetch successfull")
    import scala.util.parsing.json._
    val esraRespJSON = JSON.parseFull(esraResp)
    val modelStr = esraRespJSON.get.asInstanceOf[Map[String, Any]]("req_pack").asInstanceOf[Map[String, Any]]("model").asInstanceOf[String]
    val zipCode = esraRespJSON.get.asInstanceOf[Map[String, Any]]("req_pack").asInstanceOf[Map[String, Any]]("re_reg_rcver").asInstanceOf[Map[String, Any]]("zip_code").asInstanceOf[Map[String, Any]]("zip_main").asInstanceOf[Double]

    val modelId = rxIdMap getOrElse (modelStr.map(_.toLower), "-1")
    println (s"Model id fetch  $modelStr $modelId")

    //get esra list 
    val esraLineupRequest=s"http://esra.slingbox.com/lineup/rest/v2?correlationid=123456&uuid=$uuid&receiverid=$rxId&receiverModelId=$modelId&sourcesOrderList=STB"
    val esraLineupResp = get(esraLineupRequest,headers, 50000 , 180000, "GET")
    val esraLineupRespJSON = JSON.parseFull(esraLineupResp)
    val esraUsidListJson = esraLineupRespJSON.get.asInstanceOf[Map[String, Any]]("channels").asInstanceOf[Map[String,Any]]  
    import scala.collection.mutable.ListBuffer
    var finalEsraUsidList = new ListBuffer[String]()
    for ((k,v) <- esraUsidListJson) {
        val usid = v.asInstanceOf[Map[String,Any]]("usid").asInstanceOf[Double]
        finalEsraUsidList  += usid.toInt.toString
    }
    println (s"final esra usid list $finalEsraUsidList")

    var zeroContentIds = false
    var finalContentIdsList = contentIds.getOrElse(List())
    if(contentTitleArg.get != Option(None))//replace the above list
    {
        if(callsignArg ==  Option(None))
        {
          throw (new IllegalArgumentException("Callsign required"))
        }
        import java.net.URLEncoder
        var contentTitle = contentTitleArg.get

        val regexpSolrEscapes ="""(\:)""".r
        contentTitle = regexpSolrEscapes.replaceAllIn(contentTitle, "\\\\:")
        //val solrSpace = """\s""".r
        //contentTitle = solrSpace.replaceAllIn(contentTitle, "+")
        import java.net.URLEncoder
        contentTitle = URLEncoder.encode(contentTitle, "UTF-8");
        val callsign = callsignArg.get
        val egiTitleQuery = s"http://egi.slingbox.com/solr/dish_live_v2/select/?q=program_title:$contentTitle+AND+callsign:$callsign&fl=content_id&wt=json"
        val egiTitleQueryResp = get(egiTitleQuery,headers, 50000 , 180000, "GET")
        val egiTitleQueryRespJSON = JSON.parseFull(egiTitleQueryResp)
        val egiTitleListJson = egiTitleQueryRespJSON.get.asInstanceOf[Map[String, Any]]("response").asInstanceOf[Map[String,Any]]("docs").asInstanceOf[List[Map[String,Any]]]  
        var finalEgiContentIdsList = new ListBuffer[String]()
        egiTitleListJson.foreach (  x  => 
          finalEgiContentIdsList += (x.asInstanceOf[Map[String,Any]]("content_id").asInstanceOf[String]).toInt.toString
        ) 
        finalContentIdsList=finalEgiContentIdsList.toList    
        zeroContentIds = finalContentIdsList.forall(x => x.equals("0"))  
    }

    println (s"Final contentIds list to query $finalContentIdsList")

    if(finalContentIdsList.isEmpty) {
        println ("ERROR: NO CONTENT WITH THIS TITLE !!")
    } else {
        if(zeroContentIds){
            println ("ERROR: CONTENTIDS ARE NOT YET GENERATED!")

        } else {
            //get usids from egi
            //
            val egiContentIdQuery=s"http://egi.slingbox.com/solr/dish_live_v2/select/?q=content_id:("+finalContentIdsList.mkString("+")+")&fl=usid&wt=json"
            val egiCidResp = get(egiContentIdQuery,headers, 50000 , 180000, "GET")
            val egiCidRespJSON = JSON.parseFull(egiCidResp)
            val egiUsidListJson = egiCidRespJSON.get.asInstanceOf[Map[String, Any]]("response").asInstanceOf[Map[String,Any]]("docs").asInstanceOf[List[Map[String,Any]]]  
            var finalEgiUsidList = new ListBuffer[String]()
            egiUsidListJson.foreach (  x  => 
              finalEgiUsidList += (x.asInstanceOf[Map[String,Any]]("usid").asInstanceOf[Double]).toInt.toString
            )

            println (s"Final usid list is $finalEgiUsidList")
            //intersect and see if user has subscription for any of the usids
            val finalEsraUsidSet = finalEsraUsidList.toSet
            val finalEgiUsidSet = finalEgiUsidList.toSet
            val intersectedUsidList = finalEsraUsidSet.intersect(finalEgiUsidSet)

            if(intersectedUsidList.isEmpty)
            {
              println ("ERROR: USER NOT SUBSCRIBED TO THE CONTENT!!! AND HENCE UNAVAILABLE")
            } else {
              println ("user subscribed to the content")
              //check rubens call for lineup subscription
              //instanceId
              //zipCode
              //lineupId
              //val rubensURLTempalte = s"https://rubens.slingbox.com/rubens-dish/rest/v1/domain/dam/member/$uuid/profile/$uuid/platform/ios/device/ipad/instance/$instanceId/receiver/$rxId/zip/$zipCode/sports/schedule?csAgeLevel=0&profileType=master&lineupId=$lineupId&startTime=$startTime&endTime=$endTime&limit=1000&rsource=GF_client&correlationId=1495033726523&contentIds=$contentId&_=1495033724252"

            }
        }
  }

} catch {
    case ioe: java.io.IOException =>  ioe.printStackTrace// handle this
    case ste: java.net.SocketTimeoutException => ste.printStackTrace // handle this
}
  }

import org.apache.http.impl.client.DefaultHttpClient

def buildHttpClient() =  {
    val httpClient = new DefaultHttpClient
    httpClient
}

import org.apache.http.client.methods.HttpGet
import org.apache.http.client.config.RequestConfig

@throws(classOf[java.io.IOException])
@throws(classOf[java.net.SocketTimeoutException])
def get(url: String,headers: Map[String,String],
        connectTimeout: Int = 5000,
        readTimeout: Int = 180000,
        requestMethod: String = "GET"): String = {
    import java.net.URI
    println (s"Querying url $url")
    val httpGet = new HttpGet(url)
    httpGet.setConfig(RequestConfig.custom().setConnectTimeout(connectTimeout).setConnectionRequestTimeout(readTimeout).build());
    for ((k,v) <- headers) httpGet.setHeader(k,v)
    val httpClient = buildHttpClient()
    val httpResponse = httpClient.execute(httpGet)
    val entity = httpResponse.getEntity
    var content = ""
    if (entity != null) {
        val inputStream = entity.getContent
        content = io.Source.fromInputStream(inputStream).getLines.mkString
        inputStream.close
    }
    content
}


}






