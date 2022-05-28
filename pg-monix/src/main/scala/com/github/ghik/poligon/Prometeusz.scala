package com.github.ghik.poligon

import com.avsystem.commons._
import io.micrometer.core.instrument.{Gauge, Timer}
import io.micrometer.prometheus.{PrometheusConfig, PrometheusMeterRegistry}
import monix.execution.Scheduler
import org.eclipse.jetty.http.HttpStatus
import org.eclipse.jetty.server.handler.{AbstractHandler, ContextHandler}
import org.eclipse.jetty.server.{Request, Server}

import java.time.{Duration => JDuration}
import java.util.concurrent.TimeUnit
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import scala.concurrent.duration._

object Prometeusz {
  def main(args: Array[String]): Unit = {
    val registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

    Gauge.builder("awesomeness", () => 3.14 + math.random()).register(registry)

    val timer = Timer.builder("rekwesty")
      .publishPercentileHistogram()
      .serviceLevelObjectives(JDuration.ofMillis(100))
      .minimumExpectedValue(JDuration.ofMillis(1))
      .maximumExpectedValue(JDuration.ofSeconds(10))
      .register(registry)

    def report(): Unit = {
      timer.record((math.random() * 10000).toLong, TimeUnit.MILLISECONDS)
      Scheduler.global.scheduleOnce((math.random() * 100).millis)(report())
    }
    Scheduler.global.scheduleOnce(Duration.Zero)(report())

    val server = new Server(9000)
    server.setHandler(new ContextHandler("/metrics").setup { ch =>
      ch.setHandler(new AbstractHandler {
        def handle(
          target: String,
          baseRequest: Request,
          request: HttpServletRequest,
          response: HttpServletResponse
        ): Unit = {
          if (request.getMethod == "GET") {
            baseRequest.setHandled(true)
            response.setStatus(HttpStatus.OK_200)
            registry.scrape(response.getWriter)
          }
        }
      })
    })

    server.start()
    server.join()
  }
}
