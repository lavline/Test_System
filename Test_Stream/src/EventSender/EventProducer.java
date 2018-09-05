package EventSender;

import java.util.Properties;
import java.util.Random;
import java.util.Scanner;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

import MySerdes.ValueSerde;
import Structure.EventVal;

public class EventProducer {

	public static void main(String[] args) {
		Properties Props =  new Properties();
		Props.put("bootstrap.servers", "localhost:9092");
		Props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
		Props.put("value.serializer", ValueSerde.EventValSerde.class.getName());
		
		//创建生产者
		KafkaProducer<String, EventVal> producer = new KafkaProducer<>(Props);
		
		Scanner input = new Scanner(System.in);
		int num;
		System.out.println("输入事件数: ");
		num = input.nextInt();
		int stock_id, attribute_num;
		Random r = new Random();
		for(int i = 0; i < num; i++) {

			//System.out.println("输入stock id：");
			//stock_id = r.nextInt(10);
			stock_id = 1;
			//input.nextLine();
			//System.out.println("输入属性数目：");
			attribute_num = 20;

			//System.err.println(stock_id + " " + attribute_num);

			EventVal eVal = new EventVal(attribute_num, stock_id);
			eVal.EventProduceTime = System.nanoTime();
			//创建Record
			ProducerRecord<String, EventVal> record = new ProducerRecord<>("NewEvent", eVal);
			//发送
			try {
				producer.send(record);
				Thread.sleep(15);
			} catch (Exception e) {
				e.printStackTrace();
			} //finally {
			System.err.println("Producer Send Success!");
			//	producer.close();
			//}
		}
		producer.close();
		input.close();
	}
}
