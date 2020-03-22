package live.sidian.database.synchronizer;

import live.sidian.database.synchronizer.exception.FailInitiateException;
import live.sidian.database.synchronizer.model.Database;
import live.sidian.database.synchronizer.service.Synchronizer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.xml.crypto.Data;

@SpringBootTest
class SynchronizerApplicationTests {
    @Autowired
    Synchronizer synchronizer;
    @Resource
    Database source;
    @Resource
    Database target;

    @Test
    void contextLoads() throws FailInitiateException {
        synchronizer.sync(source,target);
    }

}
