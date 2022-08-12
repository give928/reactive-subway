package nextstep.subway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import reactor.blockhound.BlockHound;

@EnableR2dbcRepositories
@EnableR2dbcAuditing
@SpringBootApplication
public class SubwayApplication {
    public static void main(String[] args) {
        BlockHound.install(); // Java agent to detect blocking calls from non-blocking threads.

        SpringApplication.run(SubwayApplication.class, args);
    }
}
