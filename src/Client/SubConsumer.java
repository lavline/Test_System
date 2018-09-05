package Client;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Properties;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;

import MySerdes.ValueSerde;
import Structure.EventVal;

public class SubConsumer {
	
	
	public static void main(String[] args) {
		Properties props = new Properties();
		props.put("bootstrap.servers", "localhost:9092");
		props.put("group.id", "Consumer");
		props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
		props.put("value.deserializer", ValueSerde.EventValDeserde.class.getName());
		
		KafkaConsumer<String, EventVal> consumer = new KafkaConsumer<>(props);
		
		/*
		Scanner input = new Scanner(System.in);
		String name;
		name = input.next();
		input.close();
		*/
		consumer.subscribe(Arrays.asList("TestClient"));
		
		//TopicPartition topicPartition = new TopicPartition("", 0);
		
		//final CountDownLatch latch = new CountDownLatch(1);
        // attach shutdown handler to catch control-c
		
        Runtime.getRuntime().addShutdownHook(new Thread("consumer-shutdown-hook") {
            @Override
            public void run() {
                consumer.wakeup();
                //latch.countDown();
            }
        });
		BufferedWriter bw = null;
		//BufferedWriter bw1 = null;
		try {
			//读取Record
			//latch.await();
			//File file1 = new File("C:\\lzhy\\stream测试\\log13.txt");
			File file = new File("C:\\lzhy\\stream_test\\data\\rcv_time15.txt");
			FileWriter fw = new FileWriter(file, true);
			//FileWriter fw1 = new FileWriter(file1, true);
			bw = new BufferedWriter(fw);
			//bw1 = new BufferedWriter(fw1);
			long tmptime = 0;
			while(true) {
				ConsumerRecords<String, EventVal> records = consumer.poll(70);
				// 将offset挪到分区起始端
				//consumer.seekToBeginning(Collections.singleton(topicPartition));
				//consumer.seek(topicPartition, 4);
				tmptime = System.nanoTime();
				for(ConsumerRecord<String, EventVal> record : records) {

					EventVal eVal = record.value();
					eVal.EventGetTime = tmptime - eVal.EventStartSendTime;
					long tmp = tmptime - eVal.EventProduceTime;
					//System.out.println("Stock Id: " + eVal.StockId + " time " + (tmp / 1000000.0));
					String s = String.valueOf(eVal.StockId) + " " + String.valueOf(eVal.EventArriveTime / 1000000.0) + " "
							   + String.valueOf(eVal.EventMatchTime / 1000000.0) + " "
							   + String.valueOf(eVal.EventGetTime / 1000000.0) + " "
					           + String.valueOf(tmp / 1000000.0);
					bw.write(s + "\n");
					/*
					System.out.println("Stock Id: " + eVal.StockId);
					System.out.println("事件到达stream消耗时间：" + (eVal.EventArriveTime / 1000000.0) +
										" 事件匹配消耗时间：" + (eVal.EventMatchTime / 1000000.0) +
										" 事件发送消耗时间：" + (eVal.EventGetTime / 1000000.0) +
										" 事件产生到接收总时间：" + (tmp / 1000000.0));
										*/
					/*
					int n = eVal.AttributeNum;
					s = "";
					for(int i = 0; i < n; i++) {
						s = s + String.valueOf(eVal.eventVals[i].attributeId) + " " + eVal.eventVals[i].val + " ";
						//System.out.println("attribute id: " + eVal.eventVals[i].attributeId + " value: " + eVal.eventVals[i].val);
					}
					bw1.write(s + "\n");
					*/
				}
			}
			
		} catch (WakeupException e) {

		} catch (Throwable e) {
			System.exit(1);
		} finally {
			try {
				//bw.close();
				//bw1.close();
			}catch (Exception e){
				e.printStackTrace();
			}
			consumer.close();
        }
		System.exit(0);
	}
}
