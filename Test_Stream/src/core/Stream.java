package core;

import java.util.Properties;
import java.util.concurrent.CountDownLatch;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.Consumed;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.KStream;

import MySerdes.ValueSerde;

import Structure.BitSetVal;
import Structure.Bucket;
import Structure.EventVal;
import Structure.List;
import Structure.SubscribeVal;

public class Stream {

	public final static int PART = 100;
    public final static int MAX_VALUE = 1000;
    public final static double GROUP_WIDTH = (double)MAX_VALUE / (double)PART;
    public final static int STOCKNUM = 100;
    public final static int ATTRIBUTE_NUM = 20;
    public static int[] SubNum = new int[STOCKNUM];
    
    public static Bucket[][][][] bucketlist = new Bucket[STOCKNUM][ATTRIBUTE_NUM][PART][2];

    public static double AverSendTime = 0;
    public static double LastSendTime = 0;
    //public static int LastSendThreadNum = 30;
    public static int SendNum = 0;
    public static int SendThreadNum = 2;
    //public static double alpha = 1.0 / 1000000.0;

    //true:上一次是增加;false:上一次是减少
    public static boolean ThreadState = true;
    public static int ThreadState_lock = 0;
    public static boolean Update_lock = false;

    public static void AddThread(){
        if(!Update_lock) {
            if (SendThreadNum < 50)
                SendThreadNum += 2;
            ThreadState = true;
            Update_lock = true;
        }else {
            Update_lock = false;
        }
    }
    public static void SubThread(){
        if(!Update_lock) {
            if (SendThreadNum > 2)
                SendThreadNum -= 2;
            ThreadState = false;
            Update_lock = true;
        }else {
            Update_lock = false;
        }
    }
    /*
    public static void AddThread(){
        if (SendThreadNum < 50)
            SendThreadNum += 2;
        ThreadState = true;
    }
    public static void SubThread(){
        if (SendThreadNum > 2)
            SendThreadNum -= 2;
        ThreadState = false;
    }*/

    public static void main(String[] args){

		//初始化bucketlist
		for(int i = 0; i < 2; i++){
			for(int j = 0; j < ATTRIBUTE_NUM; j++){
				for(int r = 0; r < STOCKNUM; r++){
					for(int w = 0; w < PART; w++)
						bucketlist[r][j][w][i] = new Bucket();
				}
			}
		}
		//
		for(int i= 0; i < STOCKNUM; i++)
		    SubNum[i] = 0;

		//配置配置文件
        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "streams-match");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());

        final StreamsBuilder builder = new StreamsBuilder();

        KStream<String, SubscribeVal> subscribe = builder.stream("NewSub",
        		Consumed.with(Serdes.String(), new ValueSerde.SubscribeSerde()));
        KStream<String, EventVal> event = builder.stream("NewEvent",
        		Consumed.with(Serdes.String(), new ValueSerde.EventSerde()));

        BitSetVal[][] bitSet = new BitSetVal[STOCKNUM][10000];

        System.out.println("Stream Id: streams-match Max Stock Num: " + STOCKNUM + " Max Attribute Num: " + ATTRIBUTE_NUM +
                "\nPart Num: " + PART + " Max Value: " + MAX_VALUE + " Group Width: " + GROUP_WIDTH);

        //对订阅信息做处理 将订阅插入到bucketlist
        subscribe.foreach((k,v)->{

			final  String subId = v.SubId;
            final int stock_id = v.StockId;
			final  int sub_num_id = SubNum[stock_id];
			final int attributeNum = v.AttributeNum;

			System.out.println("Client Name: " + subId + " Client Num Id: " + sub_num_id +
					" Sub Stock Id: " + stock_id + " Attribute Num: " + attributeNum);

        	//将订阅添加到bitset
			bitSet[stock_id][sub_num_id] = new BitSetVal();
        	bitSet[stock_id][sub_num_id].SubId = subId;
        	bitSet[stock_id][sub_num_id].state = true;
        		
        	//将订阅插入到对应的bucketlist
        	for(Integer i = 0; i < attributeNum; i++) {
        		
        		int attribute_id = v.subVals[i].attributeId;
        		double min_val = v.subVals[i].min_val;
        		double max_val = v.subVals[i].max_val;

        		//System.out.println("Attribute Id: " + attribute_id + " Lower Limit: " + min_val + " Hight Limit: " + max_val);

        		int group = (int)(min_val / GROUP_WIDTH);
        		bucketlist[stock_id][attribute_id][group][0].bucket.add(new List(sub_num_id, min_val));
        		group = (int)(max_val / GROUP_WIDTH);
        		bucketlist[stock_id][attribute_id][group][1].bucket.add(new List(sub_num_id, max_val));
        	} 
        	
        	SubNum[stock_id]++;
        });

        //多线程测试部分
        class Parallel implements Runnable{
            public int start;
            public int end;
            public EventVal v;
            public int stock_id;
            public CountDownLatch latch;

            public Parallel(int start, int end, EventVal val, int stock_id, CountDownLatch latch){
                this.start = start;
                this.end = end;
                this.v = val;
                this.stock_id = stock_id;
                this.latch = latch;
            }
            public void run(){
                //System.out.println("从" + start + "到" + end + "的线程启动");
                for(int i = this.start; i < end; i++) {
                    int attribute_id = this.v.eventVals[i].attributeId;
                    double val = this.v.eventVals[i].val;
                    int group = (int)(val / GROUP_WIDTH);
                    //System.out.println("Attribute Id: " + attribute_id + " Val: " + val + " Group: " + group);
                    for(List e:bucketlist[this.stock_id][attribute_id][group][1].bucket) {
                        if(e.val < val) {
                            bitSet[this.stock_id][e.Id].b = true;
                        }
                    }
                    for(int j = group - 1; j >= 0; j--) {
                        for(List e:bucketlist[this.stock_id][attribute_id][j][1].bucket) {
                            bitSet[this.stock_id][e.Id].b = true;
                        }
                    }
                    for(List e:bucketlist[this.stock_id][attribute_id][group][0].bucket) {
                        if(e.val > val) {
                            bitSet[this.stock_id][e.Id].b = true;
                        }
                    }
                    for(int j = group + 1; j < PART; j++) {
                        for(List e:bucketlist[this.stock_id][attribute_id][j][0].bucket) {
                            bitSet[this.stock_id][e.Id].b = true;
                        }
                    }
                }
                this.latch.countDown();
            }
        }

        //match部分
        KStream<String, EventVal> matchstream = event.mapValues( v -> {
            //计算时间
            long tmpTime = System.nanoTime();
            //EventVal eVal = value;
            v.EventArriveTime = tmpTime - v.EventProduceTime;

        	//该部分根据bucketlist进行match得到匹配的bitset
        	final int attributeNum = v.AttributeNum;
        	final int stock_id = v.StockId;
        	//final int step = attributeNum / 2;

        	System.out.println("Stock Id: " + stock_id + " Attribute Num: " + attributeNum);
            /*
            final  CountDownLatch latch = new CountDownLatch(2);

        	Parallel p1 = new Parallel(0, step, v, stock_id, latch);
            Parallel p2 = new Parallel(step, attributeNum, v, stock_id, latch);
        	Thread t1 = new Thread(p1);
            Thread t2 = new Thread(p2);

            tmpTime = System.nanoTime();
            try {
                t1.start();
                t2.start();
                latch.await();
            } catch (InterruptedException e) {
                 e.printStackTrace();
            } //finally {
                //System.out.println("ALL线程结束");
            //}
            */

            tmpTime = System.nanoTime();
        	for(int i = 0; i < attributeNum; i++) {
        		//对event所有属性进行标记
        		int attribute_id = v.eventVals[i].attributeId;
        		double val = v.eventVals[i].val;

        		int group = (int)(val / GROUP_WIDTH);
        	    //System.out.println("Attribute Id: " + attribute_id + " Val: " + val + " Group: " + group);

        		//将该组中比event最小还小的标记
        		for(List e:bucketlist[stock_id][attribute_id][group][1].bucket) {
        			if(e.val < val) {
        				bitSet[stock_id][e.Id].b = true;
        			}
        		}
        		//将该组左边的所有组标记
        		for(int j = group - 1; j >= 0; j--) {
        			for(List e:bucketlist[stock_id][attribute_id][j][1].bucket) {
        				bitSet[stock_id][e.Id].b = true;
        			}
        		}

        		//将订阅最小比event要大的标记
        		for(List e:bucketlist[stock_id][attribute_id][group][0].bucket) {
        			if(e.val > val) {
        				bitSet[stock_id][e.Id].b = true;
        			}
        		}
        		//将该组右边的所有组标记
        		for(int j = group + 1; j < PART; j++) {
        			for(List e:bucketlist[stock_id][attribute_id][j][0].bucket) {
        				bitSet[stock_id][e.Id].b = true;
        			}
        		}
        	}

            v.EventMatchTime = System.nanoTime() - tmpTime;
        	return v;
        });

        Properties ProducerProps =  new Properties();
        ProducerProps.put("bootstrap.servers", "localhost:9092");
        ProducerProps.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        ProducerProps.put("value.serializer", ValueSerde.EventValSerde.class.getName());
        KafkaProducer<String, EventVal> producer = new KafkaProducer<>(ProducerProps);

        //发送部分
        class SendParallel implements Runnable{
            public int start;
            public int end;
            public int stock_id;
            public EventVal v;
            public CountDownLatch latch;

            public SendParallel(int start, int end, int stock_id, EventVal val, CountDownLatch latch){
                this.start = start;
                this.end = end;
                this.v = val;
                this.stock_id = stock_id;
                this.latch = latch;
            }

            public void run() {
                for (int i = this.start; i < this.end; i++) {
                    //System.out.println("检索bitset");
                    if (bitSet[this.stock_id][i].state) {
                        //System.out.println("bitset已使用");
                        if (!bitSet[this.stock_id][i].b) {
                            this.v.EventStartSendTime = System.nanoTime();
                            ProducerRecord<String, EventVal> record = new ProducerRecord<>(bitSet[this.stock_id][i].SubId, this.v);
                            try {
                                producer.send(record);
                                //System.out.println("发送成功");
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    } else {
                        bitSet[this.stock_id][i].b = false;
                    }
                }
                latch.countDown();
            }
        }

       //KStream<String, EventVal> sendstream = matchstream.peek((k,v)->{
       matchstream.foreach((k,v)->{
           long tmp = System.nanoTime();

           if(SendNum == 10){
               AverSendTime = AverSendTime / 10.0;
               System.out.println("每十个事件平均发送时间: " + AverSendTime + " 线程数: " + SendThreadNum);
               if(AverSendTime > 10) {
                   if(ThreadState_lock < 3) {
                       if (ThreadState) {
                           if (AverSendTime < LastSendTime) {
                               AddThread();
                           }
                           if (AverSendTime > LastSendTime) {
                               SubThread();
                           }
                       } else {
                           if (AverSendTime > LastSendTime) {
                               AddThread();
                           }
                           if (AverSendTime < LastSendTime) {
                               SubThread();
                           }
                       }
                   }
                   ThreadState_lock = 0;
               }else {
                   ThreadState_lock++;
               }
               LastSendTime = AverSendTime;
               AverSendTime = 0;
               SendNum = 0;
               System.out.println("动态调整花费时间:" + ((System.nanoTime() - tmp) / 1000000.0));
           }

           v.EventStartSendTime = tmp;
           int stock_id = v.StockId;
           long tmp1 = System.nanoTime();
           int step = (SubNum[stock_id] / SendThreadNum) + 1;
           SendParallel[] s = new SendParallel[SendThreadNum];
           Thread[] t = new Thread[SendThreadNum];
           final  CountDownLatch latch = new CountDownLatch(SendThreadNum);
           try {
               for (int i = 0; i < SendThreadNum; i++) {
                   s[i] = new SendParallel(i * step, ((i + 1) * step) > SubNum[stock_id] ? SubNum[stock_id] : ((i + 1) * step), stock_id, v, latch);
                   t[i] = new Thread(s[i]);
                   t[i].start();
               }
               System.out.println(System.nanoTime() - tmp1 + " " + SendThreadNum);
               latch.await();
           }catch (Exception e){
               e.printStackTrace();
           }
           /*
           int stock_id = v.StockId;
           for(int i = 0; i < SubNum[stock_id]; i++) {
               //System.out.println("检索bitset");
                if(bitSet[stock_id][i].state) {
                    //System.out.println("bitset已使用");
                    if(!bitSet[stock_id][i].b){
                        v.EventStartSendTime = System.nanoTime();
                        ProducerRecord<String, EventVal> record = new ProducerRecord<>(bitSet[stock_id][i].SubId, v);
                        try {
                            producer.send(record);
                            //System.out.println("发送成功");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }else {
                    bitSet[stock_id][i].b = false;
                }

                /*
                int n = i;
                System.out.println("检索bitset");
                //该部分根据bitset进行过滤转发event
                if(bitSet[event_stock_id][n].state) {
                    System.out.println("bitset已使用");
                    outstream.filter((k1,v1)->{
                        if(bitSet[event_stock_id][n].b) {
                            System.out.println("没有匹配");
                            bitSet[event_stock_id][n].b = false;
                            return false;
                        }
                        else {
                            System.out.println(bitSet[event_stock_id][n].SubId + " matched, start send...");
                            return true;
                        }
                    }).to(bitSet[event_stock_id][n].SubId);
                }
                */
            //}
            tmp = System.nanoTime() - tmp;
            AverSendTime = AverSendTime + (tmp / 1000000.0);
            SendNum++;

           System.out.println("发送检索消耗时间: " + tmp / 1000000.0);
        });

        final Topology topology = builder.build();
        final KafkaStreams streams = new KafkaStreams(topology, props);
        final CountDownLatch latch = new CountDownLatch(1);

        // attach shutdown handler to catch control-c
        Runtime.getRuntime().addShutdownHook(new Thread("streams-shutdown-hook") {
            @Override
            public void run() {
                streams.close();
                producer.close();
                latch.countDown();
            }
        });

        try {
            streams.start();
            latch.await();
        } catch (Throwable e) {
            System.exit(1);
        }
        System.exit(0);
    }
    
}
