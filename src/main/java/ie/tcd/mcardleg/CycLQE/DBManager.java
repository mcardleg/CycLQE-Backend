package ie.tcd.mcardleg.CycLQE;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.awscore.defaultsmode.DefaultsMode;
import software.amazon.awssdk.core.waiters.WaiterResponse;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import software.amazon.awssdk.services.dynamodb.waiters.DynamoDbWaiter;


public class DBManager {

    private DynamoDbClient dbClient = configureDbClient();

    public void createTables() {
        createTable(Constants.RAW_ACCELEROMETER_TABLE);
        createTable(Constants.RAW_GPS_TABLE);
        createTable(Constants.PROCESSED_DATA_TABLE);
        addTTL(Constants.RAW_ACCELEROMETER_TABLE);
        addTTL(Constants.RAW_GPS_TABLE);
    }

    public void deleteTables() {
        deleteTable(Constants.RAW_ACCELEROMETER_TABLE);
        deleteTable(Constants.RAW_GPS_TABLE);
        deleteTable(Constants.PROCESSED_DATA_TABLE);
        DynamoDbWaiter dbWaiter = dbClient.waiter();
        WaiterResponse<DescribeTableResponse> waiterResponse =
                dbWaiter.waitUntilTableNotExists(r -> r.tableName(Constants.PROCESSED_DATA_TABLE));
        waiterResponse.matched().response().ifPresent(System.out::println);
    }

    private DynamoDbClient configureDbClient() {
        return DynamoDbClient
                .builder()
                .defaultsMode(DefaultsMode.AUTO)
                .httpClientBuilder(UrlConnectionHttpClient.builder())
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(
                        Constants.AWS_ACCESS_KEY,
                        Constants.AWS_SECRET_ACCESS_KEY)))
                .build();
    }

    public DynamoDbClient getDbClient() {
        return dbClient;
    }

    public void createTable(String tableName) {
        if (checkTableExists(tableName)) {
            System.out.println("Table " + tableName + " already exists.");
        }
        else {
            CreateTableRequest request = CreateTableRequest.builder()
                    .keySchema(KeySchemaElement.builder()
                            .attributeName(Constants.UUID)
                            .keyType(KeyType.HASH)
                            .build())
                    .attributeDefinitions(AttributeDefinition.builder()
                            .attributeName(Constants.UUID)
                            .attributeType(ScalarAttributeType.S)
                            .build())
                    .provisionedThroughput(ProvisionedThroughput.builder()
                            .readCapacityUnits(20L)
                            .writeCapacityUnits(20L)
                            .build())
                    .tableName(tableName)
                    .build();

            dbClient.createTable(request);
            System.out.println("Creating table: " + tableName);
        }
    }

    private void addTTL(String tableName) {
        DynamoDbWaiter dbWaiter = dbClient.waiter();
        WaiterResponse<DescribeTableResponse> waiterResponse =
                dbWaiter.waitUntilTableExists(r -> r.tableName(tableName));
        waiterResponse.matched().response().ifPresent(System.out::println);

        dbClient.updateTimeToLive(UpdateTimeToLiveRequest.builder()
                .tableName(tableName)
                .timeToLiveSpecification(TimeToLiveSpecification.builder()
                        .enabled(true)
                        .attributeName(Constants.TTL)
                        .build())
                .build());
    }

    public void deleteTable(String tableName) {
        if (!checkTableExists(tableName)) {
            System.out.println("Table " + tableName + " doesn't exist.");
        }
        else {
            DeleteTableRequest request = DeleteTableRequest.builder()
                    .tableName(tableName)
                    .build();
            dbClient.deleteTable(request);
            System.out.println("Deleting table: " + tableName);
        }
    }

    public Boolean checkTableExists(String tableName) {
        if (dbClient.listTables(
                ListTablesRequest.builder().build())
                .tableNames().contains(tableName)) {
            return true;
        }
        return false;
    }
}
