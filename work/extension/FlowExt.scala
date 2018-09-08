package baile.stream.extension

import akka.NotUsed
import akka.stream.FlowShape
import akka.stream.scaladsl.{ Concat, Flow, GraphDSL, Source, Zip }
import akka.util.ByteString

import scala.concurrent.Future

object FlowExt {

  /**
    * Fold the stream and push the last B to downstream when upstream finishes.
    * @param zero
    * @param f
    * @tparam A
    * @tparam B
    * @return
    */
  def fold[A, B](zero: => B)(f: (B, A) => B): Flow[A, B, NotUsed] = {
    customStatefulProcessor[A, B, B](zero)(
      (b, a) => (Some(f(b, a)), Vector.empty),
      b => Vector(b)
    )
  }

  def rechunkByteStringBySize(chunkSize: Int): Flow[ByteString, ByteString, NotUsed] =
    Flow[ByteString].via(new Chunker(chunkSize))

  def zipWithConstantLazyAsync[A, B](futB: => Future[B]): Flow[A, (A, B), NotUsed] = {
    Flow.fromGraph( GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      val zip = builder.add(Zip[A, B]())

      SourceExt.constantLazyAsync(futB) ~> zip.in1

      FlowShape(zip.in0, zip.out)
    })
  }

  /**
    * Zip a stream with the indices of its elements.
    * @return
    */
  def zipWithIndex[A]: Flow[A, (A, Long), NotUsed] = {
    withHead(includeHeadInUpStream = false) { head =>
      Flow[A].scan((head, 0L)) { case ((_, n), el) => (el, n + 1) }
    }
  }

  /**
    * Create a Flow whose creation depends on the first element of the upstream.
    * @param includeHeadInUpStream true if we want the first element of the upstream to be included in the dowstream.
    * @param f takes the first element of upstream in input and returns the resulting flow
    * @tparam A
    * @tparam B
    * @tparam M
    * @return the flow returned by f
    */
  def withHead[A, B, M](includeHeadInUpStream: Boolean)(f: A => Flow[A, B, M]): Flow[A, B, NotUsed] = {
    Flow[A]
      .prefixAndTail(1)
      .map {
        case (Seq(), _) => Source.empty
        case (head +: _, tailStream) =>
          if (includeHeadInUpStream) {
            Source.combine(Source.single(head), tailStream)(Concat(_)).via(f(head))
          }
          else {
            tailStream.via(f(head))
          }
      }
      .flatMapConcat(identity)
  }

  /**
    * Fold and/or unfold the stream with an user-defined function.
    * @param zero initial state
    * @param f takes current state and current elem, returns a seq of C elements to push downstream and the next state b
    *          if we want the stream to continue (if no new state b, the stream ends).
    * @param lastPushIfUpstreamEnds if the upstream ends (before customStatefulProcessor decides to end the stream),
    *                               this function is called on the last b state and the resulting c elements
    *                               are pushed downstream as the last elements of the stream.
    * @return
    */
  def customStatefulProcessor[A, B, C](zero: => B)(
    f: (B, A) => (Option[B], IndexedSeq[C]),
    lastPushIfUpstreamEnds: B => IndexedSeq[C] = { _: B => IndexedSeq.empty }
  ): Flow[A, C, NotUsed] = Flow[A].via(new StatefulProcessor(zero, f, lastPushIfUpstreamEnds))

}
