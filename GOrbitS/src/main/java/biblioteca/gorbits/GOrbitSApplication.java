package biblioteca.gorbits;

import biblioteca.gorbits.config.ClientPaymentMessageProperties;
import biblioteca.gorbits.config.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({JwtProperties.class, ClientPaymentMessageProperties.class})
public class GOrbitSApplication {

    public static void main(String[] args) {
        SpringApplication.run(GOrbitSApplication.class, args);
    }

}
