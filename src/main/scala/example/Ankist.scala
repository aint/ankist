package example

import java.nio.file.{Files, Path, Paths}

import com.fasterxml.jackson.databind.{ObjectMapper, PropertyNamingStrategy}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import example.json.LinguaLeoResponse
import io.vertx.core.buffer.Buffer
import io.vertx.scala.ext.web.client.{HttpRequest, HttpResponse, WebClient}
import io.vertx.scala.core.Vertx

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.duration.Duration._
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}

object Ankist {

  val USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:47.0) Gecko/20100101 Firefox/47.0"
  val LINGUA_LEO_BASE_URL = "api.lingualeo.com"
  val LINGUA_LEO_TRANSLATE_API = "/gettranslates"
  val GOOGLE_TRANSLATE_BASE_URL = "translate.google.com"
  val GOOGLE_TRANSLATE_API = "/translate_tts?ie=UTF-8&tl=en-US&client=tw-ob"

  private val vertx = Vertx.vertx()
  private val client = WebClient.create(vertx)

  def main(args: Array[String]): Unit = {
    val word = "comprehension"

    val leoResponse = Await.result(getLinguaLeoResponse(word), Duration(3, "sec"))
    println(leoResponse)
    val picUrl = leoResponse.picUrl.getOrElse(leoResponse.translate.map(_.picUrl).filter(_.nonEmpty).head)
    val mp3Url = leoResponse.soundUrl
    println(picUrl)
    println(mp3Url)


  }

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

  private def saveAsFile(bytes: Array[Byte], fileNameWithExt: String): Unit = {
    val path = Paths.get(fileNameWithExt)
    Files.write(path, bytes)
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

  private def getLinguaLeoResponse(word: String): Future[LinguaLeoResponse] = {
    client.get(LINGUA_LEO_BASE_URL, s"$LINGUA_LEO_TRANSLATE_API?word=$word").sendFuture()
        .map(_.bodyAsBuffer())
        .map(_.get)
        .map(_.getBytes)
        .map(bytes => {
          val mapper = new ObjectMapper()
          mapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE)
          mapper.registerModule(DefaultScalaModule)

          mapper.readValue(bytes, classOf[LinguaLeoResponse])
        })
  }

  //todo extract to some helper object
  implicit class HttpRequestExt(request: HttpRequest[Buffer]) {

    implicit def returnBytesFuture(): Future[Array[Byte]] =
      request
        .sendFuture()
        .map(_.bodyAsBuffer())
        .map(_.get)             //todo: refactor this
        .map(_.getBytes)
  }

}
