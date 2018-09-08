import org.scalatest.mock.MockitoSugar
import org.scalatest.{MustMatchers, WordSpecLike}
import org.mockito.Mockito._

class BasicSpec extends WordSpecLike with MustMatchers with MockitoSugar{


  "A simpleMessage " must {
  /*  "message should be able to tell message" in {

      val mockA = mock[A]
      when(mockA.a) thenReturn 2

     val obj = new Basics()
       //override val obj = mockA

      println(">>>>>>>>>>"+obj.show())

    }*/

    "mocking shold be done properly" in {
      val oldObjB = new B
      val c = mock[C]

     // val oldObjaA = new A(oldObjB)

      val mockB = mock[B]

      when(mockB.desc) thenReturn "hiiiiiiiii"

     val obj = new A(mockB,c)

      println(">>>>>>>>>>"+obj.describe())

    }
  }
}