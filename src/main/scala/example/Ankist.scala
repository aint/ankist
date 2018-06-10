package example

import java.nio.file.Files
import java.nio.file.Paths

import com.softwaremill.sttp._
import example.json.LinguaLeoResponse
import org.json4s._
import org.json4s.DefaultFormats
import org.json4s.native.JsonMethods

object Ankist {

  val USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:47.0) Gecko/20100101 Firefox/47.0"
  val LINGUA_LEO_API = "https://api.lingualeo.com/gettranslates"
  val GOOGLE_TRANSLATE_API = "https://translate.google.com/translate_tts?ie=UTF-8&tl=en-US&client=tw-ob"

  implicit val backend = HttpURLConnectionBackend()
  implicit val formats = DefaultFormats

  def main(args: Array[String]): Unit = {
    val word = "comprehension"

    wordToMp3(downloadFile, word)

    val leoResponse = getLinguaLeoResponse(word)
    println(leoResponse)
  }

  private def wordToMp3(fun: String => Array[Byte], word: String): Unit = {
    val bytes = fun(s"$GOOGLE_TRANSLATE_API&q=$word")
    val path = Paths.get(s"$word.mp3")
    Files.write(path, bytes)
  }

  private def downloadFile(url: String): Array[Byte] = {
    val response = sttp
      .get(uri"$url")
      .header("User-Agent", USER_AGENT)
      .response(asByteArray)
      .send()

    response.body match {
      case Left(error) => throw new RuntimeException(s"Error while downloading file: $error")
      case Right(body) => body
    }
  }

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
