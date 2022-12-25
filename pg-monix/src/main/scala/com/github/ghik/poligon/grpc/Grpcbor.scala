package com.github.ghik.poligon.grpc

import com.avsystem.commons.serialization.cbor.HasCborCodec
import io.grpc.MethodDescriptor.MethodType
import io.grpc._
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder
import monix.eval.Task
import monix.execution.Scheduler
import monix.execution.Scheduler.Implicits.global
import monix.reactive.Observable

import scala.concurrent.duration._

case class FooReq(
  int: Int,
  str: String,
)
object FooReq extends HasCborCodec[FooReq]

case class FooResp(
  boo: Boolean,
  fag: Seq[Int],
)
object FooResp extends HasCborCodec[FooResp]

trait FooService {
  def foo(req: FooReq): Observable[FooResp]
}
object FooService extends CyborgRpcCompanion[FooService] {
  final val FooMethod =
    MethodDescriptor.newBuilder(RawCborMarshaller, RawCborMarshaller)
      .setIdempotent(true)
      .setFullMethodName("FooService/foo")
      .setType(MethodType.SERVER_STREAMING)
      .build()
}

object Grpcbor {
  final val ServiceName = "grpcbor"
}

object GrpcborServer {
  private val fooImpl = new FooService {
    def foo(req: FooReq): Observable[FooResp] =
      Observable.timerRepeated(Duration.Zero, 1.second, FooResp(req.int > 0, req.str.map(_.toInt)))
  }

  private val rawCyborgRpc = RawCyborgRpc.asRaw[FooService](fooImpl)

  def main(args: Array[String]): Unit = {
    val ssd = ServerServiceDefinition
      .builder("FooService")
      .addMethod(FooService.FooMethod, new CyborgRpcServerCallHandler(rawCyborgRpc))
      .build()

    val server: Server =
      NettyServerBuilder
        .forPort(6666)
        .executor(Scheduler.global)
        .addService(ssd)
        .build()

    server.start()
    server.awaitTermination()
  }
}

object GrpcborClient {
  def main(args: Array[String]): Unit = {
    val channel: ManagedChannel =
      ManagedChannelBuilder
        .forAddress("localhost", 6666)
        .usePlaintext()
        .directExecutor()
        .build()

    val rawRpc = new RawCyborgRpcClient(channel, Map("foo" -> FooService.FooMethod))
    val fooService = RawCyborgRpc.asReal[FooService](rawRpc)

    fooService.foo(FooReq(42, "foo"))
      .mapEval(r => Task(println(r)))
      .completedL.timeout(10.seconds)
      .runSyncUnsafe()

    channel.shutdown()
  }
}
