package project_two.client;

import java.util.concurrent.TimeUnit;

public class Client {
    public static void main(String[] args) throws InterruptedException {
        while(true){
            System.out.println("hey");
            TimeUnit.SECONDS.sleep(2);
        }
    }
}
