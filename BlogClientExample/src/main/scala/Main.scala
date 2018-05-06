import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import akka.util.ByteString

object Main extends App {
  implicit val system = ActorSystem("my-system")
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  val imageUrl = "https://images.unsplash.com/photo-1523306801845-480ee4445283?ixlib=rb-0.3.5&q=80&fm=jpg&crop=entropy&cs=tinysrgb&w=1080&fit=max&ixid=eyJhcHBfaWQiOjF9&s=3ec46ce5a67327bfcba15efeabc926ca"

  val getUrl = "http://localhost:8080/stream_image"

  val maybeImagePart =
    Http()
      .singleRequest(HttpRequest(uri = imageUrl))
        .map {
          case response: HttpResponse if response.status == StatusCodes.OK =>
            val stream: Source[ByteString, _] = response.entity.dataBytes
              val multipartBodyPart = Multipart.FormData.BodyPart(
                "image[]",
                HttpEntity.IndefiniteLength(MediaTypes.`application/octet-stream`, stream),
                Map("filename" -> "random.jpeg")
              )
            Right(multipartBodyPart)
          case response =>
            response.discardEntityBytes()
            Left(println("An error occured streaming the image - consuming stream"))
        }

  maybeImagePart.map {
    case Right(imageReq) =>
      val reqEntity = Multipart.FormData(imageReq).toEntity
      Http()
        .singleRequest(
          HttpRequest(
            uri = getUrl,
            method = HttpMethods.POST,
            entity = reqEntity
          )
        )
        .map {
          case response: HttpResponse if response.status == StatusCodes.OK =>
            println(s"Got an OK response from server ${response}")
            response.discardEntityBytes()
            println("Consuming stream to prevent backpressure delay...")
          case response =>
            response.discardEntityBytes()
            println(
              s"Response was not equal to 200 recieved ${response.status.intValue()} - consuming stream..."
            )
        }
        .recover {
          case error =>
            println("Some error occured...")
        }
    case _ =>
      println("Some error occured creating the Multipart Formdata...")
  }
}
