interface A {
void show();

}

interface Abcd {

void details(int x , int y);
}

class Hello {

int x = 20 ;
void my(){
int y = 10 ;

A obj = ()->System.out.println("this is simple show method and y ="+x);
x++;
obj.show();

}

void details(int x , int y){

System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
Abcd obj = (p , q)-> System.out.println("value of x = "+p+"and y = "+q);
obj.details(x,y);
}

public static void main(String...args){


Hello obj = new Hello();
//obj.my();


obj.details(1,2);

}}


