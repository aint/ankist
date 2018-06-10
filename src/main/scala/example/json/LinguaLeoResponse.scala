package example.json

case class LinguaLeoResponse(errorMsg: String,
                             translateSource: String,
                             isUser: Int,
                             wordForms: List[WordForm],
                             picUrl: Option[String],
                             translate: List[Translate],
                             transcription: String,
                             wordId: Int,
                             wordTop: Int,
                             soundUrl: String)
