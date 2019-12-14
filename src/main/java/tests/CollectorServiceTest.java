package tests;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import itsfine.com.datatimefilteredcollector.dto.ParkObject;
import itsfine.com.datatimefilteredcollector.service.CollectorService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;

import java.io.IOException;
import java.time.LocalDateTime;

@SpringBootApplication(scanBasePackages = {"itsfine.com.datatimefilteredcollector"})
@ComponentScan(basePackages = "itsfine.com.datatimefilteredcollector.service")
class CollectorServiceTest {

    private ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
    private ConfigurableApplicationContext context;
    private CollectorService service;
    private ParkObject notPaidParkObject = new ParkObject(0, "AAA", LocalDateTime.now());
    private String notPaidParkObjectStr;

    @BeforeEach
    void setUp() throws JsonProcessingException {
        context = SpringApplication.run(CollectorServiceTest.class);
        service = context.getBean(CollectorService.class);
        notPaidParkObjectStr = mapper.writeValueAsString(notPaidParkObject);
    }

    @AfterEach
    void tearDown() {
        service.finedCars.remove(
                notPaidParkObject.getParking_id() + ":" + notPaidParkObject.getCar_number());
        context.close();
    }

    @Test
    void takeValidData() throws IOException {
        service.takeValidData(notPaidParkObjectStr);
        Assertions.assertTrue(service.parkedCars.containsKey(
                notPaidParkObject.getParking_id() + ":" + notPaidParkObject.getCar_number()));
        notPaidParkObject.setDate_time(LocalDateTime.now().plusMinutes(11));
        notPaidParkObjectStr = mapper.writeValueAsString(notPaidParkObject);
        service.takeValidData(notPaidParkObjectStr);
        Assertions.assertFalse(service.parkedCars.containsKey(
                notPaidParkObject.getParking_id() + ":" + notPaidParkObject.getCar_number()));
        service.finedCars.put(notPaidParkObject.getParking_id() + ":" + notPaidParkObject.getCar_number(),
                notPaidParkObject.getDate_time().minusMinutes(11));
        notPaidParkObject.setDate_time(LocalDateTime.now().plusHours(25));
        notPaidParkObjectStr = mapper.writeValueAsString(notPaidParkObject);
        service.takeValidData(notPaidParkObjectStr);
        Assertions.assertFalse(service.finedCars.containsKey(
                notPaidParkObject.getParking_id() + ":" + notPaidParkObject.getCar_number()));
    }

    @Test
    void takeNotPaidCar() throws IOException {
        service.takeNotPaidCar(notPaidParkObjectStr);
        Assertions.assertTrue(service.finedCars.containsKey(
                notPaidParkObject.getParking_id() + ":" + notPaidParkObject.getCar_number()));
    }

    @Test
    void clearNotParkedCars() {
        service.parkedCars.put(notPaidParkObject.getParking_id() + ":" + notPaidParkObject.getCar_number(),
                LocalDateTime.now().minusMinutes(13));
        service.parkedCars.put(notPaidParkObject.getParking_id() + ":BBB",
                LocalDateTime.now().minusMinutes(11));
        service.clearNotParkedCars();
        Assertions.assertTrue(service.parkedCars.containsKey(notPaidParkObject.getParking_id() + ":BBB"));
        Assertions.assertFalse(service.parkedCars.containsKey(
                notPaidParkObject.getParking_id() + ":" + notPaidParkObject.getCar_number()));
    }

    @Test
    void clearFinedCars() {
        service.finedCars.put(notPaidParkObject.getParking_id() + ":" + notPaidParkObject.getCar_number(),
                LocalDateTime.now().minusHours(25));
        service.finedCars.put(notPaidParkObject.getParking_id() + ":BBB",
                LocalDateTime.now().minusHours(23));
        service.clearFinedCars();
        Assertions.assertTrue(service.finedCars.containsKey(notPaidParkObject.getParking_id() + ":BBB"));
        Assertions.assertFalse(service.finedCars.containsKey(
                notPaidParkObject.getParking_id() + ":" + notPaidParkObject.getCar_number()));
    }
}