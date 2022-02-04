package com.github.ghik.poligon

import com.avsystem.commons._
import com.avsystem.commons.misc.Timestamp
import com.avsystem.commons.serialization.HasGenCodec
import com.avsystem.commons.serialization.json.{JsonStringInput, JsonStringOutput}

import java.io.FileWriter
import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter
import java.time.{LocalDateTime, ZoneId}
import scala.annotation.tailrec
import scala.collection.Factory
import scala.concurrent.duration._
import scala.io.Source
import scala.util.Using

case class CovidCase(
  date: Timestamp,
  voivodship: String,
  district: String,
  age: Opt[Int],
  male: Boolean,
  vaccinated: Boolean,
  count: Int,
)
object CovidCase extends HasGenCodec[CovidCase]

case class CovidDeath(
  date: Timestamp,
  male: Boolean,
  comorbidity: Boolean,
  vaccinated: Boolean,
  immunocompromised: Boolean,
  voivodship: String,
  district: String,
  age: Opt[Int],
  count: Int,
)
object CovidDeath extends HasGenCodec[CovidDeath]

object Kowidy {
  val df = new SimpleDateFormat("dd.MM.yyyy")

  lazy val csvCases: Vector[CovidCase] =
    Using.resource(Source.fromFile("/Users/ghik/Downloads/zakazenia.csv", "cp1250")) { s =>
      s.getLines()
        .drop(1)
        .map(_.split(";"))
        .map {
          case Array(dateStr, voivodship, district, ageStr, sexStr, vaccStr, countStr) =>
            CovidCase(
              df.parse(dateStr).toTimestamp,
              voivodship,
              district,
              ageStr.toIntOption.toOpt,
              sexStr == "M",
              vaccStr == "T",
              countStr.toInt
            )
          case arr =>
            throw new Exception(s"malformed line ${arr.mkString(";")}")
        }
        .toVector
        .sortBy(_.date)
    }

  lazy val csvDeaths: Vector[CovidDeath] =
    Using.resource(Source.fromFile("/Users/ghik/Downloads/zgony.csv", "cp1250")) { s =>
      s.getLines()
        .drop(1)
        .map(_.split(";"))
        .map {
          case Array(dateStr, sexStr, comorbStr, vaccStr, imcompStr, voivodship, district, ageStr, countStr) =>
            CovidDeath(
              df.parse(dateStr).toTimestamp,
              sexStr == "M",
              comorbStr == "T",
              vaccStr == "T",
              imcompStr == "T",
              voivodship,
              district,
              ageStr.toIntOption.toOpt,
              countStr.toInt
            )
          case arr =>
            throw new Exception(s"malformed line ${arr.mkString(";")}")
        }
        .toVector
        .sortBy(_.date)
    }

  lazy val jsonCases: Vector[CovidCase] =
    JsonStringInput.read[Vector[CovidCase]](Using.resource(Source.fromFile("zakazenia.json"))(_.mkString))

  lazy val jsonDeaths: Vector[CovidDeath] =
    JsonStringInput.read[Vector[CovidDeath]](Using.resource(Source.fromFile("zgony.json"))(_.mkString))

  def process(): Unit = {
    Using.resource(new FileWriter("zakazenia.json"))(_.write(JsonStringOutput.write(csvCases)))
    Using.resource(new FileWriter("zgony.json"))(_.write(JsonStringOutput.write(csvDeaths)))
  }

  implicit class collectionOps[C[X] <: Iterable[X], A](private val coll: C[A]) extends AnyVal {
    def groupAdjacentBy[K](f: A => K)(implicit
      fac: Factory[A, C[A]],
      groupFac: Factory[(K, C[A]), C[(K, C[A])]]
    ): C[(K, C[A])] = {
      val it = coll.iterator
      val builder = groupFac.newBuilder

      @tailrec def loop(lastKey: Opt[K], subBuilder: MBuilder[A, C[A]]): Unit =
        if (it.hasNext) {
          val next = it.next()
          val key = f(next)
          lastKey match {
            case Opt(prevKey) if prevKey != key =>
              builder += ((prevKey, subBuilder.result()))
              val newSubBuilder = fac.newBuilder
              newSubBuilder += next
              loop(Opt(key), newSubBuilder)
            case _ =>
              subBuilder += next
              loop(Opt(key), subBuilder)
          }
        } else lastKey.foreach { key =>
          val lastResult = subBuilder.result()
          if (lastResult.nonEmpty) {
            builder += ((key, lastResult))
          }
        }

      loop(Opt.Empty, fac.newBuilder)
      builder.result()
    }
  }

  def main(args: Array[String]): Unit = {
    val startDate = jsonDeaths.head.date

    val minDate = jsonDeaths.last.date - 97.days

    def periodStart(death: CovidDeath, period: FiniteDuration): Timestamp = {
      val millisIntoWeek = startDate.until(death.date).toMillis % period.toMillis
      death.date - millisIntoWeek.millis
    }

    val df = DateTimeFormatter.ofPattern("dd.MM")
    jsonDeaths.filter(d => d.age.exists(_ >= 70) && d.date >= minDate)
      .groupAdjacentBy(_ => minDate)
      .foreach { case (weekStart, deaths) =>
        val vacRate = 0.7
        val date = LocalDateTime.ofInstant(weekStart.toInstant, ZoneId.systemDefault()).format(df)
        val unvaccDeaths = deaths.view.filterNot(_.vaccinated).map(_.count).sum
        val vaccDeaths = deaths.view.filter(_.vaccinated).map(_.count).sum
        val ratio = (unvaccDeaths.toDouble / (1 - vacRate)) / (vaccDeaths.toDouble / vacRate)
        println(f"$date: $unvaccDeaths unvaccinated, $vaccDeaths vaccinated ($ratio ratio)")
      }
  }
}
