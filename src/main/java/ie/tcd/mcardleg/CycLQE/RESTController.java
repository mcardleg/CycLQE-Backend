package ie.tcd.mcardleg.CycLQE;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.text.ParseException;
import java.util.concurrent.TimeUnit;

@RestController
public class RESTController {
    private final DBManager DBManager = new DBManager();
    private Boolean run;

    @GetMapping("/createdb")
    public String createDB() {
        DBManager.createTables();

        return "Created DynamoDB tables.";
    }

    @GetMapping("/deletedb")
    public String deleteDB() {
        DBManager.deleteTables();

        return "Deleted table";
    }

    @GetMapping("/run")
    public String startProcessing() throws ParseException, InterruptedException {
        run = true;
            while(run) {
                DataProcessor dataProcessor = new DataProcessor(DBManager);
                dataProcessor.process();
                System.out.println("Iteration complete.");
                TimeUnit.MINUTES.sleep(5);
            }

        return "Processed table.";
    }

    @GetMapping("/stop")
    public String stopProcessing() {
        run = false;

        return "Server informed to not run processor again.";
    }

}
