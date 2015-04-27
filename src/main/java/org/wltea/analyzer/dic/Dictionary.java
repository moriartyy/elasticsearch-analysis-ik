/**
 * IK 中文分词  版本 5.0
 * IK Analyzer release 5.0
 * 
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * 源代码由林良益(linliangyi2005@gmail.com)提供
 * 版权声明 2012，乌龙茶工作室
 * provided by Linliangyi and copyright 2012 by Oolong studio
 * 
 * 
 */
package org.wltea.analyzer.dic;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;
import org.wltea.analyzer.cfg.Configuration;

/**
 * 词典管理类,单子模式
 */
public class Dictionary {


    private final static Map<String, Timestamp> dictsLastLoad = new HashMap<String, Timestamp>(); 
	/*
	 * 词典单子实例
	 */
	private static Dictionary singleton;

    private DictSegment _MainDict = new DictSegment((char)0);

    private DictSegment _SurnameDict = new DictSegment((char)0);

    private DictSegment _QuantifierDict = new DictSegment((char)0);

    private DictSegment _SuffixDict = new DictSegment((char)0);

    private DictSegment _PrepDict = new DictSegment((char)0);

    private DictSegment _StopWords = new DictSegment((char)0);

	
	/**
	 * 配置对象
	 */
	private Configuration configuration;
    public static ESLogger logger = Loggers.getLogger("ik-analyzer");
    private static ScheduledExecutorService pool = Executors.newScheduledThreadPool(1);
    
    public static final String DIC_MAIN = "dic_main";
    public static final String DIC_SURNAME = "dic_surname";
    public static final String DIC_QUANTIFIER = "dic_quantifier";
    public static final String DIC_SUFFIX = "dic_suffix";
    public static final String DIC_PREP = "dic_preposition";
    public static final String DIC_STOP = "dic_stopword";
    
    private Dictionary(){

    }
    
    public void relaod() {
        this.loadMain();
    }

	public static synchronized Dictionary initial(Configuration cfg){
		if(singleton == null){
			synchronized(Dictionary.class){
				if(singleton == null){
					singleton = new Dictionary();
                    singleton.configuration=cfg;
                    singleton.loadDicts();
                    pool.scheduleWithFixedDelay(new Runnable() {
                        
                        @Override
                        public void run() {
                            singleton.loadMain();
                        }
                    }, 60, 60, TimeUnit.SECONDS);
	                return singleton;
				}
			}
		}
		return singleton;
	}
	
	private void loadMain() {
	    this.loadDict(DIC_MAIN, _MainDict);
	}
	
	private void loadDicts() {
        this.loadDict(DIC_MAIN, _MainDict);
        this.loadDict(DIC_SURNAME, _SurnameDict);
        this.loadDict(DIC_QUANTIFIER, _QuantifierDict);
        this.loadDict(DIC_SUFFIX, _SuffixDict);
        this.loadDict(DIC_PREP, _PrepDict);
        this.loadDict(DIC_STOP, _StopWords);        
    }

    private void loadDict(String tableName, DictSegment dictSegment) {
        Timestamp loadStart = new Timestamp(System.currentTimeMillis());
	    Timestamp lastLoad = dictsLastLoad.get(tableName);
	    boolean firstLoad = false;
	    if (lastLoad == null) {
	        lastLoad = new Timestamp(0);
	        firstLoad = true;
	    }
	    if (logger.isDebugEnabled()) {
	        logger.debug("Loading dict " + tableName + " since " + lastLoad);
	    }
	    Connection connection = null;
	    PreparedStatement statement = null;
	    ResultSet resultSet = null;
	    try {
            connection = DriverManager.getConnection(
                    this.configuration.getDatabaseConnectionUrl(),
                    this.configuration.getDatabaseUsername(),
                    this.configuration.getDatabaseUserpass());
            
            String sql = String.format("select word, ts from %s where ts>=? order by ts", tableName);
            statement = connection.prepareStatement(sql);
            statement.setTimestamp(1, lastLoad);
            resultSet = statement.executeQuery();
            while (resultSet.next()) {
                if (logger.isInfoEnabled() && !firstLoad) {
                    logger.info("Add word " + resultSet.getString("word"));
                }
                dictSegment.fillSegment(resultSet.getString("word").toCharArray());
                lastLoad = resultSet.getTimestamp("ts");
            }
            dictsLastLoad.put(tableName, loadStart);
        } catch (Exception e) {
            logger.error("ik-analyzer",e);
        } finally {
            if (resultSet != null) {
                try {
                    resultSet.close();
                } catch (SQLException e) {
                }
            }
            if (statement != null) {
                try {
                    statement.close();
                } catch (SQLException e) {
                }
            }
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                }
            }
            if (logger.isDebugEnabled()) {
                logger.debug("Dicts loaded.");
            }
            firstLoad = false;      
        }
	}
	
	/**
	 * 获取词典单子实例
	 * @return Dictionary 单例对象
	 */
	public static Dictionary getSingleton(){
		if(singleton == null){
			throw new IllegalStateException("词典尚未初始化，请先调用initial方法");
		}
		return singleton;
	}
	
	/**
	 * 检索匹配主词典
	 * @return Hit 匹配结果描述
	 */
	public Hit matchInMainDict(char[] charArray){
		return singleton._MainDict.match(charArray);
	}
	
	/**
	 * 检索匹配主词典
	 * @return Hit 匹配结果描述
	 */
	public Hit matchInMainDict(char[] charArray , int begin, int length){
        return singleton._MainDict.match(charArray, begin, length);
	}
	
	/**
	 * 检索匹配量词词典
	 * @return Hit 匹配结果描述
	 */
	public Hit matchInQuantifierDict(char[] charArray , int begin, int length){
		return singleton._QuantifierDict.match(charArray, begin, length);
	}
	
	
	/**
	 * 从已匹配的Hit中直接取出DictSegment，继续向下匹配
	 * @return Hit
	 */
	public Hit matchWithHit(char[] charArray , int currentIndex , Hit matchedHit){
		DictSegment ds = matchedHit.getMatchedDictSegment();
		return ds.match(charArray, currentIndex, 1 , matchedHit);
	}
	
	
	/**
	 * 判断是否是停止词
	 * @return boolean
	 */
	public boolean isStopWord(char[] charArray , int begin, int length){			
		return singleton._StopWords.match(charArray, begin, length).isMatch();
	}	
}
