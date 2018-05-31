package example

import java.nio.file.Files

import com.softwaremill.sttp._
import org.json4s._
import org.json4s.DefaultFormats
import org.json4s.native.JsonMethods

object Ankist {

  val USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:47.0) Gecko/20100101 Firefox/47.0"
  val LINGUA_LEO_API = "https://api.lingualeo.com/gettranslates"

  def main(args: Array[String]): Unit = {
    val word = "comprehension"

    val request = sttp
      .get(uri"https://translate.google.com/translate_tts?ie=UTF-8&tl=en-US&client=tw-ob&q=$word")
      .header("User-Agent", USER_AGENT)
      .response(asByteArray)

    implicit val backend = HttpURLConnectionBackend()
    val response = request.send()

    import java.nio.file.Paths
    val path = Paths.get(s"$word.mp3")
    val body = response.body.right.get
    Files.write(path, body)
  }

  private def getLinguaLeoResponse(word: String) = {
    implicit val formats = DefaultFormats

    def parseJson(json: String): LinguaLeoResponse = JsonMethods.parse(json).camelizeKeys.extract[LinguaLeoResponse]
    val asJson: ResponseAs[LinguaLeoResponse, Nothing] = asString.map(parseJson)

    val request = sttp
      .get(uri"$LINGUA_LEO_API?word=$word")
      .header("User-Agent", USER_AGENT)
      .response(asJson)

    implicit val backend = HttpURLConnectionBackend()
    val response = request.send()

    println(response.body)
  }


  case class LinguaLeoResponse(
    errorMsg: String,
    translateSource: String,
    isUser: Int,
    wordForms: List[WordForm],
    picUrl: String,
    translate: List[Translate],
    transcription: String,
    wordId: Int,
    wordTop: Int,
    soundUrl: String)

  case class Translate(
    id: Int,
    value: String,
    votes: Int,
    isUser: Int,
    picUrl: String)

  case class WordForm(word: String, `type`: String)

}
