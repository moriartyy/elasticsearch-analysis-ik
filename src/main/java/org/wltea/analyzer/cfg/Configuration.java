/**
 * 
 */
package org.wltea.analyzer.cfg;

import java.io.*;
import java.util.InvalidPropertiesFormatException;
import java.util.Properties;

import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.env.Environment;

public class Configuration {

    private static String FILE_NAME = "ik/IKAnalyzer.cfg.xml";
    private static ESLogger logger = null;
    private Properties properties;
    private Environment environment;

    public Configuration(Environment env){
        logger = Loggers.getLogger("ik-analyzer");
        this.properties = new Properties();
        this.environment = env;

        File fileConfig= new File(environment.configFile(), FILE_NAME);

        InputStream input = null;
        try {
            input = new FileInputStream(fileConfig);
        } catch (FileNotFoundException e) {
            logger.error("ik-analyzer",e);
        }
        if(input != null){
            try {
                properties.loadFromXML(input);
            } catch (InvalidPropertiesFormatException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public String getDatabaseConnectionUrl() {
        return String.format("jdbc:mysql://%s:%s/%s", 
                properties.getProperty("dict.db.host"), 
                properties.getProperty("dict.db.port"),
                properties.getProperty("dict.db.name"));
    }
    
    public String getDatabaseUsername() {
        return properties.getProperty("dict.db.username");
    }
    
    public String getDatabaseUserpass() {
        return properties.getProperty("dict.db.userpass");
    }
    
}
