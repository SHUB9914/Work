class B{
  def desc() = "hello"
}

class C {
  val abc = 1
}


class A(b:B , c:C) {
  //val conf = b.desc()
  val a = 10
  def describe() = b.desc()
}


class Basics {
  val b = new B()
  val c = new C()
  val obj = new A(b,c)
  def show() = obj.a
}


