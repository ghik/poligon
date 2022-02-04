package com.github.ghik.poligon

import com.mongodb.reactivestreams.client.{MongoClient, MongoClients}
import monix.eval.Task
import monix.execution.Scheduler

import scala.concurrent.Future
import scala.util.Success

object SyncLogic {

  // blocking API to communicate with the device
  trait DeviceNetworkApi {
    def getParam(param: String): Int
    def setParam(param: String, value: Int): Unit
  }

  // business logic
  def updateDevice(device: DeviceNetworkApi): Int = {
    val paramName = "counter"
    val counter = device.getParam(paramName)
    Thread.sleep(1000)
    val newCounter = counter + 1
    device.setParam(paramName, newCounter)
    newCounter
  }

}

object CallbackLogic {

  trait DeviceNetworkApi {
    def getParam(param: String)(callback: Int => Unit): Unit
    def setParam(param: String, value: Int)(callback: Unit => Unit): Unit
  }

  object Sleeper {
    def sleep(millis: Long)(callback: Unit => Unit): Unit = ???
  }

  // business logic
  def updateDevice(device: DeviceNetworkApi)(callback: Int => Unit): Unit = {
    val paramName = "counter"
    device.getParam(paramName) { counter =>
      Sleeper.sleep(1000) { _ =>
        val newCounter = counter + 1
        device.setParam(paramName, newCounter) { _ =>
          callback(newCounter)
        }
      }
    }
  }

}

object CallbackTaskLogic {

  type Callback[-T] = T => Unit
  type Task[+T] = Callback[T] => Unit

  trait DeviceNetworkApi {
    def getParam(param: String): Task[Int]
    def setParam(param: String, value: Int): Task[Unit]
  }

  object Sleeper {
    def sleep(millis: Long): Task[Unit] = ???
  }

  // business logic
  def updateDevice(device: DeviceNetworkApi): Task[Int] = { callback =>
    val paramName = "counter"
    device.getParam(paramName) { counter =>
      Sleeper.sleep(1000) { _ =>
        val newCounter = counter + 1
        device.setParam(paramName, newCounter) { _ =>
          callback(newCounter)
        }
      }
    }
  }

}

object WrapperTaskLogic {

  type Callback[-T] = T => Unit

  object Task {
    def pure[T](value: T): Task[T] =
      Task(callback => callback(value))
  }
  case class Task[+T](exec: Callback[T] => Unit) extends AnyVal {
    def flatMap[U](f: T => Task[U]): Task[U] =
      Task(callback => exec(t => f(t).exec(callback)))

    def map[U](f: T => U): Task[U] = flatMap(t => Task.pure(f(t)))
  }

  trait DeviceNetworkApi {
    def getParam(param: String): Task[Int]
    def setParam(param: String, value: Int): Task[Unit]
  }

  object Sleeper {
    def sleep(millis: Long): Task[Unit] = ???
  }

  // business logic
  def updateDevice(device: DeviceNetworkApi): Task[Int] = {
    val paramName = "counter"
    device.getParam(paramName).flatMap { counter =>
      Sleeper.sleep(1000).flatMap { _ =>
        val newCounter = counter + 1
        device.setParam(paramName, newCounter).map(_ => newCounter)
      }
    }
  }

  // business logic
  def updateDeviceFC(device: DeviceNetworkApi): Task[Int] =
    for {
      paramName <- Task.pure("counter")
      counter <- device.getParam(paramName)
      _ <- Sleeper.sleep(1000)
      newCounter = counter + 1
      _ <- device.setParam(paramName, newCounter)
    } yield newCounter

}

object ExecutorWrapperTaskLogic {

  import java.util.concurrent.Executor

  type Callback[-T] = T => Unit

  object Task {
    def pure[T](value: T): Task[T] =
      Task((executor, callback) => executor.execute(() => callback(value)))
  }
  case class Task[+T](exec: (Executor, Callback[T]) => Unit) extends AnyVal {
    def flatMap[U](f: T => Task[U]): Task[U] =
      Task((executor, callback) => exec(executor, t => f(t).exec(executor, callback)))

    def map[U](f: T => U): Task[U] = flatMap(t => Task.pure(f(t)))
  }

}

object TaskConceptually {

  trait Task[+T] {
    def runAsync(
      cb: Either[Throwable, T] => Unit
    )(implicit s: Scheduler): Cancelable
  }

  trait Cancelable {
    def cancel(): Unit
  }

}

object TaskExamples {

  import monix.eval.Task

  val justReturn42: Task[Int] = Task.now(42)
  val greet: Task[Unit] = Task(println("greeting"))
  val fail: Task[Nothing] = Task.raiseError(throw new Exception("not good"))

  val someRawTask: Task[Unit] =
    Task.async0 { (scheduler, callback) =>
      // call some low-level Java callback-based API
    }

  val taskFromFuture: Task[Unit] =
    Task.deferFutureAction(implicit scheduler => Future(println("from Future")))

  import Scheduler.Implicits.global

  greet.runSyncUnsafe() // blocking

  greet.runAsync {
    case Right(_) =>
    case Left(failure) => failure.printStackTrace()
  }

  greet.runAsyncAndForget

  val greetFuture: Future[Unit] = greet.runToFuture

}

object RefTrans {

  import Scheduler.Implicits.global

  val greeter: Task[Unit] =
    Task(println("greetings!")) // nothing

  greeter.runSyncUnsafe() // greetings!
  greeter.runSyncUnsafe() // greetings!

  val someTask: Task[Unit] = for {
    _ <- Task(println("greetings!")) // greetings! is printed here
    _ <- Task(println("greetings!")) // greetings! is printed here
  } yield ()

  val greet: Task[Unit] =
    Task(println("greetings!")) // nothing happens here

  val theSameTask: Task[Unit] = for {
    _ <- greet // greetings! is printed here
    _ <- greet // greetings! is printed here
  } yield ()

}

object Futuresy {
  import scala.concurrent.ExecutionContext.Implicits.global

  val someFuture: Future[Unit] = for {
    _ <- Future(println("greetings!")) // greetings! is printed here
    _ <- Future(println("greetings!")) // greetings! is printed here
  } yield ()

  val greet: Future[Unit] =
    Future(println("greetings!")) // greetings! is printed here

  val notTheSameFuture: Future[Unit] = for {
    _ <- greet // nothing happens here
    _ <- greet // nothing happens here
  } yield ()

}

object Mongos {

  import com.avsystem.commons.misc._
  import com.avsystem.commons.mongo.typed._
  import monix.eval.Task
  import monix.execution.Scheduler
  import monix.reactive.Observable

  case class User(
    id: String,
    login: String,
    created: Timestamp,
    commentCount: Int,
  ) extends MongoEntity[String]
  object User extends MongoEntityCompanion[User]

  val mongoClient: MongoClient = MongoClients.create()
  val collection: TypedMongoCollection[User] = new TypedMongoCollection(
    mongoClient.getDatabase("test").getCollection("users")
  )

  val users: Observable[User] = {
    val minCreationTime = Timestamp.parse("2020-11-01T00:00:00Z")
    collection.find(User.ref(_.created) > minCreationTime)
  }

  case class CountAndSum(count: Int, sum: Long) {
    def avg: Double = sum.toDouble / count
  }

  val avgCommentCount: Task[Double] =
    users
      .map(_.commentCount)
      .foldLeftL(CountAndSum(0, 0)) {
        case (CountAndSum(count, sum), commentCount) =>
          CountAndSum(count + 1, sum + commentCount)
      }
      .map(_.avg)

  def main(args: Array[String]): Unit = {
    import Scheduler.Implicits.global

    avgCommentCount
      .foreachL(r => println(s"Average comment count is $r"))
      .runSyncUnsafe()
  }

  trait Publisher[+T] {
    def subscribe(subscriber: Subscriber[T]): Unit
  }

  trait Subscriber[-T] {
    def onSubscribe(subscription: Subscription): Unit
    def onNext(item: T): Unit
    def onError(throwable: Throwable): Unit
    def onComplete(): Unit
  }

  trait Subscription {
    def request(n: Long): Unit
    def cancel(): Unit
  }

  trait Processor[-T, +R] extends Subscriber[T] with Publisher[R]

}

trait Observing {

  trait Observable[+T] {
    def subscribe(subscribe: Subscriber[T]): Cancelable
  }

  trait Subscriber[-T] {
    def onNext(elem: T): Future[Ack]
    def onError(ex: Throwable): Unit
    def onComplete(): Unit
    implicit def scheduler: Scheduler
  }

  sealed trait Ack
  case object Continue extends Ack
  case object Stop extends Ack

  trait Cancelable {
    def cancel(): Unit
  }

}
