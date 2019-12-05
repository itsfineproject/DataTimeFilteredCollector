package itsfine.com.datatimefilteredcollector.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import itsfine.com.datatimefilteredcollector.dto.ParkObject;
import itsfine.com.datatimefilteredcollector.interfaces.ICollector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.support.MessageBuilder;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

@EnableBinding(ICollector.class)
public class CollectorService {

    private static final String NOT_PAID_ROUT_INPUT = "not_paid_rout";
    private ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
    private Map<String, LocalDateTime> parkedCars = new HashMap<>();
    private Map<String, LocalDateTime> finedCars = new HashMap<>();

    private long fineInterval = 24L;
    private long freeParkingDuration = 10L;

    private final
    ICollector collector;

    @Autowired
    public CollectorService(ICollector collector) {
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                clearNotParkedCars();
            }
        }, 0, 1000 * 60 * freeParkingDuration);
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                clearFinedCars();
            }
        }, 0, 1000 * 60 * 60 * fineInterval);
        this.collector = collector;
    }

    @StreamListener(ICollector.INPUT)
    void takeValidData(String validData) throws IOException {
        ParkObject parkObject = mapper.readValue(validData, ParkObject.class);
        String key = parkObject.getParking_id()+":"+parkObject.getCar_number();
        if (!finedCars.containsKey(key)) {
            if (parkedCars.containsKey(key) &&
                    parkObject.getDate_time().minusMinutes(freeParkingDuration).compareTo(parkedCars.get(key)) > 0) {
                parkObject.setDate_time(parkedCars.get(key));
                parkedCars.remove(key);
                collector.output().send(MessageBuilder.withPayload(parkObject.toString()).build());
            }
        } else if (parkObject.getDate_time().minusHours(fineInterval).compareTo(finedCars.get(key)) > 0){
            finedCars.remove(key);
        }
    }

    @StreamListener(NOT_PAID_ROUT_INPUT)
    void takeNotPaidCar(String notPaidCar) throws IOException {
        ParkObject parkObject = mapper.readValue(notPaidCar, ParkObject.class);
        finedCars.put(parkObject.getParking_id()+":"+parkObject.getCar_number(), parkObject.getDate_time());
    }

    void clearNotParkedCars(){
        Map<String, LocalDateTime> parkedCarsSnapshot = parkedCars;
        parkedCarsSnapshot.keySet()
                .stream()
                .filter(key -> parkedCarsSnapshot.get(key).minusMinutes(freeParkingDuration).compareTo(LocalDateTime.now()) > 0)
                .forEach(key -> parkedCars.remove(key));
    }

    void clearFinedCars() {
        Map<String, LocalDateTime> finedCarsSnapshot = finedCars;
        finedCarsSnapshot.keySet()
                .stream()
                .filter(key -> finedCarsSnapshot.get(key).minusHours(fineInterval).compareTo(LocalDateTime.now()) > 0)
                .forEach(key -> finedCars.remove(key));
    }
}
