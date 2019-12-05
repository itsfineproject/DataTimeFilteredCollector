package itsfine.com.datatimefilteredcollector.interfaces;

import org.springframework.cloud.stream.annotation.Input;
import org.springframework.cloud.stream.messaging.Processor;
import org.springframework.messaging.SubscribableChannel;

public interface ICollector extends Processor {
    @Input("not_paid_rout")
    SubscribableChannel not_paid_rout();
}
