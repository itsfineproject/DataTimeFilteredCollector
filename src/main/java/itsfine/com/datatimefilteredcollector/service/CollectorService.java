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
import java.util.*;

@EnableBinding(ICollector.class)
public class CollectorService {

    private static final String NOT_PAID_ROUT_INPUT = "not_paid_rout";
    private final long FINE_INTERVAL;
    private final long FREE_PARKING_DURATION;
    private ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
    public Map<String, LocalDateTime> parkedCars = new HashMap<>();
    public Map<String, LocalDateTime> finedCars = new HashMap<>();

    private final
    ICollector collector;

    @Autowired
    public CollectorService(ICollector collector, @Value("${fineInterval}") long fineInterval,
                            @Value("${freeParkingDuration}") long freeParkingDuration) {
        this.FINE_INTERVAL = fineInterval;
        this.FREE_PARKING_DURATION = freeParkingDuration;
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                clearNotParkedCars();
            }
        }, 0, 1000 * 60 * freeParkingDuration);
        Timer timer2 = new Timer();
        timer2.schedule(new TimerTask() {
            @Override
            public void run() {
                clearFinedCars();
            }
        }, 0, 1000 * 60 * 60 * fineInterval);
        this.collector = collector;
    }

    @StreamListener(ICollector.INPUT)
    public void takeValidData(String validData) throws IOException {
        ParkObject parkObject = mapper.readValue(validData, ParkObject.class);
        String key = parkObject.getParking_id() + ":" + parkObject.getCar_number();
        if (!finedCars.containsKey(key)) {
            if (!parkedCars.containsKey(key)) {
                parkedCars.put(key, parkObject.getDate_time());
            } else if (parkObject.getDate_time().compareTo(parkedCars.get(key).plusMinutes(FREE_PARKING_DURATION)) > 0) {
                parkObject.setDate_time(parkedCars.get(key));
                parkedCars.remove(key);
                collector.output().send(MessageBuilder.withPayload(parkObject.toString()).build());
            }
        } else if (parkObject.getDate_time().compareTo(finedCars.get(key).plusHours(FINE_INTERVAL)) > 0) {
            finedCars.remove(key);
        }
    }

    @StreamListener(NOT_PAID_ROUT_INPUT)
    public void takeNotPaidCar(String notPaidCar) throws IOException {
        ParkObject parkObject = mapper.readValue(notPaidCar, ParkObject.class);
        finedCars.put(parkObject.getParking_id() + ":" + parkObject.getCar_number(), parkObject.getDate_time());
    }

    public void clearNotParkedCars() {
        ArrayList<String> keysToRemove = new ArrayList<>();
        parkedCars.keySet()
                .stream()
                .filter(key -> parkedCars.get(key)
                        .compareTo(LocalDateTime.now().minusMinutes(FREE_PARKING_DURATION + 2)) < 0)
                .forEach(keysToRemove::add);
        for (String key : keysToRemove) {
            parkedCars.remove(key);
        }
    }

    public void clearFinedCars() {
        ArrayList<String> keysToRemove = new ArrayList<>();
        finedCars.keySet()
                .stream()
                .filter(key -> finedCars.get(key).
                        compareTo(LocalDateTime.now().minusHours(FINE_INTERVAL)) < 0)
                .forEach(keysToRemove::add);
        for (String key : keysToRemove) {
            finedCars.remove(key);
        }
    }


}
