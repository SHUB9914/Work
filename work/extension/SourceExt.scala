package baile.stream.extension

import akka.stream.scaladsl.Source
import akka.{ Done, NotUsed }

import scala.concurrent.Future

object SourceExt {

  /**
    * Create an infinite source of the same Async Lazy value evaluated only when the stream is materialized.
    *
    * @param fut
    * @tparam A
    * @return
    */
  def constantLazyAsync[A](fut: => Future[A]): Source[A, NotUsed] =
    Source.unfoldResourceAsync[A, A](
      { () => fut },
      { a => Future.successful(Some(a)) },
      { _ => Future.successful(Done) }
    )

}
