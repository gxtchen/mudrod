/*
 * Licensed under the Apache License, Version 2.0 (the "License"); you 
 * may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package esiptestbed.mudrod.weblog.pre;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.elasticsearch.action.index.IndexRequest;

import esiptestbed.mudrod.discoveryengine.DiscoveryStepAbstract;
import esiptestbed.mudrod.driver.ESDriver;
import esiptestbed.mudrod.driver.SparkDriver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImportLogFile extends DiscoveryStepAbstract{

  private static final Logger LOG = LoggerFactory.getLogger(ImportLogFile.class);

  /**
   * 
   */
  private static final long serialVersionUID = 1L;

  public ImportLogFile(Map<String, String> config, ESDriver es, SparkDriver spark) {
    super(config, es, spark);
  }

  @Override
  public Object execute() {
    LOG.info("*****************Import starts******************");
    startTime=System.currentTimeMillis();
    readFile();
    endTime=System.currentTimeMillis();
    LOG.info("*****************Import ends******************Took {}s", (endTime-startTime)/1000);
    es.refreshIndex();
    return null;
  }

  String logEntryPattern = "^([\\d.]+) (\\S+) (\\S+) \\[([\\w:/]+\\s[+\\-]\\d{4})\\] \"(.+?)\" (\\d{3}) (\\d+|-) \"((?:[^\"]|\")+)\" \"([^\"]+)\"";

  public static final int NUM_FIELDS = 9;
  Pattern p = Pattern.compile(logEntryPattern);
  Matcher matcher;

  public String SwithtoNum(String time){
    if (time.contains("Jan")){
      time = time.replace("Jan", "1");   
    }else if (time.contains("Feb")){
      time = time.replace("Feb", "2");   
    }else if (time.contains("Mar")){
      time = time.replace("Mar", "3");   
    }else if (time.contains("Apr")){
      time = time.replace("Apr", "4");   
    }else if (time.contains("May")){
      time = time.replace("May", "5");   
    }else if (time.contains("Jun")){
      time = time.replace("Jun", "6");   
    }else if (time.contains("Jul")){
      time = time.replace("Jul", "7");   
    }else if (time.contains("Aug")){
      time = time.replace("Aug", "8");   
    }else if (time.contains("Sep")){
      time = time.replace("Sep", "9");   
    }else if (time.contains("Oct")){
      time = time.replace("Oct", "10");   
    }else if (time.contains("Nov")){
      time = time.replace("Nov", "11");
    }else if (time.contains("Dec")){
      time = time.replace("Dec", "12");
    }

    return time;
  }

  public void readFile(){
    es.createBulkProcesser();

    String httplogpath = config.get("logDir") + config.get("httpPrefix") 
    + config.get("TimeSuffix") + "/" + config.get("httpPrefix") + config.get("TimeSuffix");
    String ftplogpath = config.get("logDir") + config.get("ftpPrefix") 
    + config.get("TimeSuffix") + "/" + config.get("ftpPrefix") + config.get("TimeSuffix");

    try {
      ReadLogFile(httplogpath, "http", config.get("indexName"), this.HTTP_type);
      ReadLogFile(ftplogpath, "FTP", config.get("indexName"), this.FTP_type);
    
    } catch (IOException e) {
      e.printStackTrace();
    } 
    es.destroyBulkProcessor();

  }

  public void ReadLogFile(String fileName, String Type, String index, String type) throws IOException{
    BufferedReader br = new BufferedReader(new FileReader(fileName));
    int count =0;
    try {
      String line = br.readLine();
      while (line != null) {	
        if(Type.equals("FTP"))
        {
          ParseSingleLineFTP(line, index, type);
        }else{
          ParseSingleLineHTTP(line, index, type);
        }

        line = br.readLine();
        count++;
      }
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }finally {
      br.close();

      LOG.info("Num of {}: {}", Type, count);
    }
  }

  public void ParseSingleLineFTP(String log, String index, String type){
    String ip = log.split(" +")[6];

    String time = log.split(" +")[1] + ":"+log.split(" +")[2] +":"+log.split(" +")[3]+":"+log.split(" +")[4];

    time = SwithtoNum(time);
    SimpleDateFormat formatter = new SimpleDateFormat("MM:dd:HH:mm:ss:yyyy");
    Date date = null;
	try {
		date = formatter.parse(time);
	} catch (ParseException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
    String bytes = log.split(" +")[7];

    String request = log.split(" +")[8].toLowerCase();

    if(!request.contains("/misc/") && !request.contains("readme"))
    {
      IndexRequest ir;
	try {
		ir = new IndexRequest(index, type).source(jsonBuilder()
		      .startObject()
		      .field("LogType", "ftp")
		      .field("IP", ip)
		      .field("Time", date)
		      .field("Request", request)
		      .field("Bytes", Long.parseLong(bytes))
		      .endObject());
		es.bulkProcessor.add(ir);
	} catch (NumberFormatException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}

      
    }

  }

  public void ParseSingleLineHTTP(String log, String index, String type){
    matcher = p.matcher(log);
    if (!matcher.matches() || 
        NUM_FIELDS != matcher.groupCount()) {
      return;
    }
    String time = matcher.group(4);
    time = SwithtoNum(time);
    SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy:HH:mm:ss");
    Date date = null;
	try {
		date = formatter.parse(time);
	} catch (ParseException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}

    String bytes = matcher.group(7);
    if("-".equals(bytes)){
      bytes="0";
    }

    String request = matcher.group(5).toLowerCase();
    String agent = matcher.group(9);
    CrawlerDetection crawlerDe = new CrawlerDetection(this.config, this.es, this.spark);
    if(crawlerDe.CheckKnownCrawler(agent))
    {

    }
    else
    {
      if(request.contains(".js")||request.contains(".css")||request.contains(".jpg")||request.contains(".png")||request.contains(".ico")||
          request.contains("image_captcha")||request.contains("autocomplete")||request.contains(".gif")||
          request.contains("/alldata/")||request.contains("/api/")||request.equals("get / http/1.1")||
          request.contains(".jpeg")||request.contains("/ws/"))   //request.contains("/ws/")  need to be discussed
      {

      }else{
        IndexRequest ir;
		try {
			ir = new IndexRequest(index, type).source(jsonBuilder()
			    .startObject()
			    .field("LogType", "PO.DAAC")
			    .field("IP", matcher.group(1))
			    .field("Time", date)
			    .field("Request", matcher.group(5))
			    .field("Response", matcher.group(6))
			    .field("Bytes", Integer.parseInt(bytes))
			    .field("Referer", matcher.group(8))
			    .field("Browser", matcher.group(9))
			    .endObject());
			
			es.bulkProcessor.add(ir);
		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

        

      }
    }
  }

  @Override
  public Object execute(Object o) {
    return null;
  }
}