import java.nio.file.Paths

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.{Directives, Route}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{FileIO, Sink, Source}
import akka.util.ByteString

import scala.io.StdIn

case class Order(email: String, amount: String)

object WebServer extends Directives {

  implicit val system = ActorSystem("my-system")
  implicit val materializer = ActorMaterializer()
  // needed for the future flatMap/onComplete in the end
  implicit val executionContext = system.dispatcher

  private def streamImage = path("stream_image") {
    post {
      entity(as[Multipart.FormData]) { recievedData: Multipart.FormData =>
        val imageDownloadResp = recievedData.parts.map { part =>
          println("Extracting the stream from Multipart Formdata...")
          val imageStream: Source[ByteString, _] = part.entity.dataBytes
          imageStream.runWith(FileIO.toPath(Paths.get("/Users/rajdeep.kambo/Downloads/random.jpeg")))
        }.runWith(Sink.ignore).map(_ => "Saved image..")

        complete(imageDownloadResp)
      }
    }
  }

  private def displayImage = path("display_image") {
    get {
      val imageStream = FileIO.fromPath(Paths.get("/Users/rajdeep.kambo/Downloads/random.jpeg"))
      val entity = HttpEntity(MediaTypes.`image/jpeg`, imageStream)
      println("About to return stream...")
      complete(entity)
    }
  }

  def main(args: Array[String]) {

    lazy val routes: Route = streamImage ~ displayImage
    val bindingFuture = Http().bindAndHandle(routes, "localhost", 8080)

    println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
    StdIn.readLine() // let it run until user presses return
    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ => system.terminate()) // and shutdown when done
  }
}
