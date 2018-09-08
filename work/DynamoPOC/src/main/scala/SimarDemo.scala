/*
import com.amazonaws.services.dynamodbv2.document.{Item, Table}
import com.amazonaws.services.ec2.model.Purchase

/**
  * Created by shubham on 1/24/18.
  */
case class Purchase(id: Int, Nomenclature: String)

class SimarDemo {


  def storeJSON(purchase: Purchase, table: Table) = {
    table.putItem(new Item().withPrimaryKey("Id", purchase.id, "Nomenclature", purchase.nomenclature))
  }

  val table = createTable()
  storeJSON(Purchase(1, "simar"), table)


}
*/
