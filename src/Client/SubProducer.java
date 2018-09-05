package Client;

import java.util.Properties;
import java.util.Random;
import java.util.Scanner;

import MySerdes.ValueSerde;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

import Structure.SubscribeVal;

public class SubProducer {

	public static void main(String[] args) {
		Properties Props =  new Properties();
		Props.put("bootstrap.servers", "localhost:9092");
		Props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
		Props.put("value.serializer", ValueSerde.SubValSerde.class.getName());
		
		//创建生产者
		KafkaProducer<String, SubscribeVal> producer = new KafkaProducer<>(Props);
		
		Scanner input = new Scanner(System.in);
		int num;
		System.out.println("输入订阅数: ");
		num = input.nextInt();
		String name = "TestClient";
		System.out.println("Client Name:" + name);
		int attribute_num, stock_id;
		Random r =new Random();
		for(int i = 0; i < num; i++) {
			//name = input.next();
			//System.out.println("Stock Id:");
			//stock_id = r.nextInt(10);
			stock_id = 1;
			//System.out.println("Attribute Num:");
			attribute_num = r.nextInt(19) + 1;
			SubscribeVal sVal;
			if(i % 33 == 0)
				sVal = new SubscribeVal(attribute_num, name, stock_id, 2);
			else {
				sVal = new SubscribeVal(attribute_num, name, stock_id);
			}
			//创建Record
			ProducerRecord<String, SubscribeVal> record = new ProducerRecord<String, SubscribeVal>("NewSub", sVal);

			//发送
			try {
				producer.send(record).get();
				System.err.println("Producer Send Success!");
			} catch (Exception e) {
				e.printStackTrace();
			}// finally {
			//	producer.close();
			//}
		}
		producer.close();
		input.close();
	}
}
