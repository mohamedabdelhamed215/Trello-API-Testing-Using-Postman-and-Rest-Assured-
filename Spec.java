/*package utils;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.specification.RequestSpecification;

public class Spec {
    private static RequestSpecification request;
    public  static RequestSpecification getRequestSpec(){
        request = new RequestSpecBuilder().addQueryParam("key", API_KEY)
                .queryParam("token", API_TOKEN)
                .header("Accept", "application/json")
                .contentType("application/json")
                .log().ifValidationFails();

    }
}
*/