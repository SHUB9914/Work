/**
  * Created by shubham on 7/5/17.
  */
object SubArrayDemo extends App {

  val list = List(1,2,3,4,5,6,7,8)
  list.map{x=>
    val subList = list.dropWhile(_!=x).drop(1)
    val result = findSubArray(List(x),subList,1,List(),10)
    println("result="+result)
    subList.map(y=>if((x+y)==10) println(List(x,y)))
  }
  def findSubArray(list1:List[Int],list2:List[Int],pos:Int,result:List[List[Int]],sum:Int):List[List[Int]]={
    val subList = list2.take(pos)
    if(subList.size==list2.size){
      result} else {
      val total = (list1:::subList).sum
      if(total==sum){
      }
      if(total==sum) findSubArray(list1 , list2 , pos+1 ,result:+(list1:::subList),sum) else
        findSubArray(list1,list2,pos+1 , result,sum)
    }

  }

}
