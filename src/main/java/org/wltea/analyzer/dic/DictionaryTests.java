package org.wltea.analyzer.dic;

import static org.elasticsearch.common.settings.ImmutableSettings.Builder.EMPTY_SETTINGS;

import java.util.concurrent.CountDownLatch;

import org.elasticsearch.common.collect.Tuple;
import org.elasticsearch.common.logging.log4j.LogConfigurator;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.node.internal.InternalSettingsPreparer;
import org.wltea.analyzer.cfg.Configuration;

public class DictionaryTests {
    
    private static volatile Thread keepAliveThread;
    private static volatile CountDownLatch keepAliveLatch;
    
    public static void main(String[] args) {
        Tuple<Settings, Environment> aa = InternalSettingsPreparer.prepareSettings(EMPTY_SETTINGS, true);
        Settings settings=aa.v1();
        Environment environment = aa.v2();
        LogConfigurator.configure(settings);
        Dictionary.initial(new Configuration(environment));
        
        keepAliveLatch = new CountDownLatch(1);
        // keep this thread alive (non daemon thread) until we shutdown
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                keepAliveLatch.countDown();
            }
        });

        keepAliveThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    keepAliveLatch.await();
                    System.out.println("@exited");
                } catch (InterruptedException e) {
                    // bail out
                }
            }
        }, "search-service[keepAlive]");
        keepAliveThread.setDaemon(false);
        keepAliveThread.start();
    }
}
