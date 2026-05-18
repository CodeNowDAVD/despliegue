package biblioteca.gorbits.config;

import java.util.concurrent.atomic.AtomicBoolean;
import javax.sql.DataSource;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Aplica el parche de esquema H2 en el primer {@link DataSource} antes de que JPA lo use.
 */
@Component
public class H2SchemaMigrationBeanPostProcessor implements BeanPostProcessor {

    private final Environment environment;
    private final AtomicBoolean applied = new AtomicBoolean();

    public H2SchemaMigrationBeanPostProcessor(Environment environment) {
        this.environment = environment;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (!(bean instanceof DataSource) || applied.get()) {
            return bean;
        }
        if (applied.compareAndSet(false, true)) {
            new H2LegacySchemaMigration((DataSource) bean, environment).apply();
        }
        return bean;
    }
}
