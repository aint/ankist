package example

import java.nio.file.Files

import com.softwaremill.sttp._

object Ankist {

  val USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:47.0) Gecko/20100101 Firefox/47.0"

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

}
