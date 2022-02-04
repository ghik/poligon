package com.github.ghik.poligon

import com.avsystem.commons._
import io.udash.rest.jetty.JettyRestClient
import io.udash.rest.{DefaultRestApiCompanion, RestServlet}
import org.eclipse.jetty.client.HttpClient
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.{ServletContextHandler, ServletHolder}

import java.util.concurrent.Executors
import scala.concurrent.Await
import scala.concurrent.duration._

trait Stuff {
  def wait(seconds: Int): Future[String]
}
object Stuff extends DefaultRestApiCompanion[Stuff]

object StuffImpl extends Stuff {
  implicit val executionContext: ExecutionContext =
    ExecutionContext.fromExecutorService(Executors.newCachedThreadPool())

  override def wait(seconds: Int): Future[String] = Future {
    println("starting")
    Thread.sleep(seconds * 1000)
    println("ending")
    s"waited $seconds"
  }
}

object Main {
  def main(args: Array[String]): Unit = {
    val server = new Server(8080)
    val handler = new ServletContextHandler
    handler.addServlet(new ServletHolder(RestServlet[Stuff](StuffImpl, 9.seconds)), "/*")
    server.setHandler(handler)
    server.start()
    server.join()
  }
}

object ClientMain {
  def main(args: Array[String]): Unit = {
    val httpClient = new HttpClient().setup(_.start())
    val client = JettyRestClient[Stuff](httpClient, "http://localhost:8080")

    val fst = client.wait(10)
    println(Try(Await.result(fst, Duration.Inf)))
    val snd = client.wait(10)
    println(Try(Await.result(snd, Duration.Inf)))

    httpClient.stop()
  }
}
