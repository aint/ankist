package example

import java.nio.file.Files
import java.nio.file.Paths

import com.softwaremill.sttp._
import example.json.LinguaLeoResponse
import io.vertx.scala.core.Vertx
import io.vertx.scala.ext.web.client.WebClient
import org.json4s._
import org.json4s.DefaultFormats
import org.json4s.native.JsonMethods

import scala.concurrent.ExecutionContext.Implicits.global

import scala.util.{Failure, Success}

object Ankist {

  val USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:47.0) Gecko/20100101 Firefox/47.0"
  val LINGUA_LEO_API = "http://api.lingualeo.com/gettranslates"
  val GOOGLE_TRANSLATE_BASE_URL = "translate.google.com"
  val GOOGLE_TRANSLATE_API = "/translate_tts?ie=UTF-8&tl=en-US&client=tw-ob"

  implicit val backend = HttpURLConnectionBackend()
  implicit val formats = DefaultFormats

  private val vertx = Vertx.vertx()
  private val client = WebClient.create(vertx)

  def main(args: Array[String]): Unit = {
    val word = "comprehension"

    wordToMp3(word)

    val leoResponse = getLinguaLeoResponse(word)
    println(leoResponse)
  }

  }

  private def wordToPng(fun: String => Array[Byte], pngUrl: Option[String], word: String): Unit = {
  private def wordToMp3(word: String): Unit = {
    client.get(GOOGLE_TRANSLATE_BASE_URL, s"$GOOGLE_TRANSLATE_API&q=$word")
      .sendFuture()
      .map(_.bodyAsBuffer)
      .onComplete{
        case Success(result) =>
          val bytes = result.get.getBytes
          val path = Paths.get(s"$word.mp3")
          Files.write(path, bytes)
        case Failure(cause) =>
          println(s"$cause")
      }
  }

  }

//  private def downloadFile(url: (String, String)): Array[Byte] = {
//      client
//        .get(url._1, url._2)
//        .send(ar => {
//          if (ar.failed()) {
//            val bytes = ar.result.bodyAsBuffer().get.getBytes
//          }
//          else ar.cause.printStackTrace()
//        })
//    //TODO add USER_AGENT
//    //TODO handle exception
//    //TODO return Option
//  }

  private def getLinguaLeoResponse(word: String): LinguaLeoResponse = {
    def parseJson(json: String): LinguaLeoResponse = JsonMethods.parse(json).camelizeKeys.extract[LinguaLeoResponse]
    val asJson: ResponseAs[LinguaLeoResponse, Nothing] = asString.map(parseJson)

    val response = sttp
      .get(uri"$LINGUA_LEO_API?word=$word")
      .header("User-Agent", USER_AGENT)
      .response(asJson)
      .send()

    response.body match {
      case Left(error) => throw new RuntimeException(s"Error while requesting LinguaLeo: $error")
      case Right(body) => body
    }
  }

}
