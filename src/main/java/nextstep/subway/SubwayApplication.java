package nextstep.subway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import reactor.blockhound.BlockHound;

@SpringBootApplication
public class SubwayApplication {
    public static void main(String[] args) {
        BlockHound.install(); // Java agent to detect blocking calls from non-blocking threads.

        SpringApplication.run(SubwayApplication.class, args);
    }
}
