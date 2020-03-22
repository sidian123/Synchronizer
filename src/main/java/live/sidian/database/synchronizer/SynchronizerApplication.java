package live.sidian.database.synchronizer;

import live.sidian.database.synchronizer.model.Database;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class SynchronizerApplication {
    @Bean
    @ConfigurationProperties(prefix = "dev.source")
    Database source(){
        return new Database();
    }

    @Bean
    @ConfigurationProperties(prefix = "dev.target")
    Database target(){
        return new Database();
    }

    public static void main(String[] args) {
        SpringApplication.run(SynchronizerApplication.class, args);
    }

}
