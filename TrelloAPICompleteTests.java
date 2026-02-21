
import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.*;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TrelloAPICompleteTests {

    //CONFIGURATION
    private static final String BASE_URI = "https://api.trello.com";
    private static final String API_VERSION = "1";
    private static final String API_KEY = "5c012a8a0920ce829df9e226c2fb09bc";
    private static final String API_TOKEN = "ATTA5503faafd12695062d3103345d8ad6d80d925f8d28b4bb995e17c79cb9bb9e6832EB8433";

    // SHARED TEST DATA
    private String boardId;
    private String listId;
    private String cardId;

    // SETUP & TEARDOWN
    @BeforeAll
    public void setup() {
        RestAssured.baseURI = BASE_URI;
        RestAssured.basePath = "/" + API_VERSION;
    }
    @AfterAll
    public void cleanup() {
        if (boardId != null && !boardId.isEmpty()) {
            try {
                deleteBoard(boardId);
                System.out.println("✓ Cleaned up test board: " + boardId);
            } catch (Exception e) {
                System.out.println("✗ Cleanup failed: " + e.getMessage());
            }
        }
    }
    private RequestSpecification getBaseRequest() {
        return given()
                .queryParam("key", API_KEY)
                .queryParam("token", API_TOKEN)
                .header("Accept", "application/json")
                .contentType("application/json")
                .log().ifValidationFails();
    }

    private RequestSpecification getInvalidTokenRequest() {
        return given()
                .queryParam("key", API_KEY)
                .queryParam("token", "ATTA5503fssfd12695062d3103345d8ad6d80d925f8d38b4bb995e17c79cb9bb9e6832EB8433")
                .contentType("application/json");
    }

    private RequestSpecification getInvalidKeyRequest() {
        return given()
                .queryParam("key", "5c012a8a0920dds829df9e226c2fb09bc")
                .queryParam("token", API_TOKEN)
                .contentType("application/json");
    }

    private void deleteBoard(String id) {
        getBaseRequest()
                .pathParam("id", id)
                .delete("/boards/{id}");
    }
    // 01 - AUTHENTICATION TESTS (TC01-TC03)
    @Test
    @Order(1)
    @DisplayName("TC01 - Valid Authentication")
    public void tc01_validAuthentication() {
        Response response = getBaseRequest()
                .get("/members/me");
        // Validate status code
        response.then()
                .body("username", notNullValue())
                .log().ifError();
        // Additional assertion
            Assertions.assertEquals(200,response.getStatusCode());
        String username = response.jsonPath().getString("username");
        assertNotNull(username, "Username should not be null");
    }
    @Test
    @Order(2)
    @DisplayName("TC02 - Invalid Token")
    public void tc02_invalidToken() {
        Response response = getInvalidTokenRequest()
                .get("/members/me");
        response.then().log().ifError();
        Assertions.assertEquals(401,response.getStatusCode());
    }

    @Test
    @Order(3)
    @DisplayName("TC03 - Invalid API Key")
    public void tc03_invalidAPIKey() {
        Response response = getInvalidKeyRequest()
                .get("/members/me");
        response.then().log().ifError();
        Assertions.assertEquals(401,response.getStatusCode());
    }
    // 02 - BOARD TESTS (TC04-TC09, TC23)
    @Test
    @Order(4)
    @DisplayName("TC04 - Create Board")
    public void tc04_createBoard() {
        Response response = getBaseRequest()
                .queryParam("name", "MyBoard")
                .post("/boards");
        response.then().body("id", notNullValue()).body("name", equalTo("MyBoard")).log().ifError();
        Assertions.assertEquals(200,response.getStatusCode());
        boardId = response.jsonPath().getString("id");
        assertNotNull(boardId, "Board ID should not be null");
    }
    @Test
    @Order(5)
    @DisplayName("TC05 - Get Board")
    public void tc05_getBoard() {
        Response response = getBaseRequest()
                .pathParam("id", boardId)
                .get("/boards/{id}");
        response.then()
                .statusCode(200)
                .body("id", equalTo(boardId))
                .log().ifError();
    }
    @Test
    @Order(6)
    @DisplayName("TC06 - Get Board Details (Enhanced Validations)")
    public void tc06_getBoardDetails() {
        Response response = getBaseRequest()
                .pathParam("id", boardId)
                .get("/boards/{id}");
        // Test 1: Status code is 200
        assertEquals(200, response.getStatusCode(), "Status code should be 200");
        // Test 2: The response contains the correct board ID
        Map<String, Object> jsonData = response.jsonPath().getMap("$");
        assertEquals(boardId, jsonData.get("id"), "Board ID should match");
        // Test 3: Response has required fields
        assertNotNull(jsonData.get("name"), "Name field should exist");
        assertNotNull(jsonData.get("desc"), "Desc field should exist");
        assertNotNull(jsonData.get("prefs"), "Prefs field should exist");
        assertNotNull(jsonData.get("url"), "URL field should exist");
        // Test 4: prefs.permissionLevel is valid
        Map<String, Object> prefs = response.jsonPath().getMap("prefs");
        assertNotNull(prefs, "Prefs should not be null");
        String permissionLevel = (String) prefs.get("permissionLevel");
        assertTrue(Arrays.asList("private", "org", "public").contains(permissionLevel),
                "Permission level should be valid: " + permissionLevel);
        // Test 5: labelNames contains standard color keys
        Map<String, Object> labelNames = response.jsonPath().getMap("labelNames");
        assertNotNull(labelNames, "labelNames should not be null");
        List<String> standardColors = Arrays.asList(
                "green", "yellow", "orange", "red", "purple", "blue",
                "sky", "lime", "pink", "black",
                "green_dark", "yellow_dark", "orange_dark", "red_dark",
                "purple_dark", "blue_dark", "sky_dark", "lime_dark",
                "pink_dark", "black_dark",
                "green_light", "yellow_light", "orange_light", "red_light",
                "purple_light", "blue_light", "sky_light", "lime_light",
                "pink_light", "black_light"
        );
        for (String color : standardColors) {
            assertTrue(labelNames.containsKey(color),
                    "labelNames should contain color: " + color);
        }
    }
    @Test
    @Order(7)
    @DisplayName("TC07 - Update Board Name")
    public void tc07_updateBoardName() {
        Response response = getBaseRequest()
                .pathParam("id", boardId)
                .queryParam("name", "UpdatedBoard")
                .put("/boards/{id}");
        response.then()
                .statusCode(200)
                .body("name", equalTo("UpdatedBoard"))
                .log().ifError();
    }
    @Test
    @Order(8)
    @DisplayName("TC08 - Get Board Details After Update")
    public void tc08_getBoardDetailsAfterUpdate() {
        Response response = getBaseRequest()
                .pathParam("id", boardId)
                .get("/boards/{id}");
        response.then()
                .statusCode(200)
                .body("id", equalTo(boardId))
                .body("name", equalTo("UpdatedBoard"))
                .log().ifError();
    }
    @Test
    @Order(9)
    @DisplayName("TC09 - Get All Boards")
    public void tc09_getAllBoards() {
        Response response = getBaseRequest()
                .get("/members/me/boards");
        response.then()
                .statusCode(200)
                .body("$", not(empty()))
                .log().ifError();
        List<Map<String, Object>> boards = response.jsonPath().getList("$");
    }
    // 03 - LIST TESTS (TC10-TC14)
    @Test
    @Order(10)
    @DisplayName("TC10 - Create List")
    public void tc10_createList() {
        Response response = getBaseRequest()
                .queryParam("name", "MyList")
                .queryParam("idBoard", boardId)
                .post("/lists");
        response.then()
                .statusCode(200)
                .body("id", notNullValue())
                .body("name", equalTo("MyList"))
                .log().ifError();
        listId = response.jsonPath().getString("id");
    }
    @Test
    @Order(11)
    @DisplayName("TC11 - Get List")
    public void tc11_getList() {
        Response response = getBaseRequest()
                .pathParam("id", listId)
                .get("/lists/{id}");
        response.then()
                .statusCode(200)
                .body("id", equalTo(listId))
                .log().ifError();
    }
    @Test
    @Order(12)
    @DisplayName("TC12 - Update List Name")
    public void tc12_updateListName() {
        String newListName = "newLIstNAme";
        Response response = getBaseRequest()
                .pathParam("id", listId)
                .queryParam("name", newListName)
                .put("/lists/{id}");
        response.then()
                .statusCode(200)
                .body("name", equalTo(newListName))
                .log().ifError();
        // Enhanced validation - verify name matches request
        String actualName = response.jsonPath().getString("name");
        assertEquals(newListName, actualName, "List name should match the updated name");
    }
    @Test
    @Order(13)
    @DisplayName("TC13 - Get List After Update")
    public void tc13_getListAfterUpdate() {
        Response response = getBaseRequest()
                .pathParam("id", listId)
                .get("/lists/{id}");
        response.then()
                .statusCode(200)
                .body("id", equalTo(listId))
                .body("name", equalTo("newLIstNAme"))
                .log().ifError();
    }
    @Test
    @Order(14)
    @DisplayName("TC14 - Archive List")
    public void tc14_archiveList() {
        Response response = getBaseRequest()
                .pathParam("id", listId)
                .queryParam("closed", "true")
                .put("/lists/{id}");

        response.then()
                .statusCode(200)
                .body("closed", equalTo(true))
                .log().ifError();
        // Create new list for card tests
        Response newListResponse = getBaseRequest()
                .queryParam("name", "CardTestList")
                .queryParam("idBoard", boardId)
                .post("/lists");
        listId = newListResponse.jsonPath().getString("id");
    }
    // 04 - CARD TESTS (TC15-TC21)
    @Test
    @Order(15)
    @DisplayName("TC15 - Create Card")
    public void tc15_createCard() {
        Response response = getBaseRequest()
                .queryParam("name", "MyCard")
                .queryParam("idList", listId)
                .post("/cards");
        response.then()
                .statusCode(200)
                .body("id", notNullValue())
                .body("name", equalTo("MyCard"))
                .log().ifError();
        cardId = response.jsonPath().getString("id");
    }
    @Test
    @Order(16)
    @DisplayName("TC16 - Get Cards in List")
    public void tc16_getCardsInList() {
        Response response = getBaseRequest()
                .pathParam("id", listId)
                .get("/lists/{id}/cards");
        response.then()
                .statusCode(200)
                .body("$", not(empty()))
                .log().ifError();
        List<Map<String, Object>> cards = response.jsonPath().getList("$");
    }
    @Test
    @Order(17)
    @DisplayName("TC17 - Delete Card (with Pre-request)")
    public void tc17_deleteCard() {
        // Pre-request: Create a card to delete
        String cardName = "Card to be Deleted - " + System.currentTimeMillis();
        Response createResponse = getBaseRequest()
                .queryParam("name", cardName)
                .queryParam("idList", listId)
                .post("/cards");
        String tempCardId = createResponse.jsonPath().getString("id");
        // Main request: Delete the card
        Response response = getBaseRequest()
                .pathParam("id", tempCardId)
                .delete("/cards/{id}");
        // Validation
        int statusCode = response.getStatusCode();
        assertTrue(statusCode == 200 || statusCode == 204,
                "Status code should be 200 or 204, but was: " + statusCode);
        if (statusCode == 200) {
            Map<String, Object> json = response.jsonPath().getMap("$");
            assertTrue(json.containsKey("limits"), "Response should contain 'limits' key");
            Map<String, Object> limits = (Map<String, Object>) json.get("limits");
            assertTrue(limits.isEmpty(), "Limits should be empty: {}");
        }
    }

    @Test
    @Order(18)
    @DisplayName("TC18 - Create Card (for subsequent tests)")
    public void tc18_createCard() {
        Response response = getBaseRequest()
                .queryParam("name", "MyCard")
                .queryParam("idList", listId)
                .post("/cards");
        response.then()
                .statusCode(200)
                .body("id", notNullValue())
                .body("name", equalTo("MyCard"))
                .log().ifError();
        cardId = response.jsonPath().getString("id");
    }

    @Test
    @Order(19)
    @DisplayName("TC19 - Update Card Name")
    public void tc19_updateCardName() {
        Response response = getBaseRequest()
                .pathParam("id", cardId)
                .queryParam("name", "UpdatedCard")
                .put("/cards/{id}");

        response.then()
                .statusCode(200)
                .body("name", equalTo("UpdatedCard"))
                .log().ifError();
    }
    @Test
    @Order(20)
    @DisplayName("TC20 - Get Cards After Update")
    public void tc20_getCardsAfterUpdate() {
        Response response = getBaseRequest()
                .pathParam("id", listId)
                .get("/lists/{id}/cards");
        response.then()
                .statusCode(200)
                .body("$", not(empty()))
                .log().ifError();
    }

    @Test
    @Order(21)
    @DisplayName("TC21 - Delete Card (Final)")
    public void tc21_deleteCardFinal() {
        String cardName = "Card to be Deleted - " + System.currentTimeMillis();
        Response createResponse = getBaseRequest()
                .queryParam("name", cardName)
                .queryParam("idList", listId)
                .post("/cards");
        String tempCardId = createResponse.jsonPath().getString("id");
        Response response = getBaseRequest()
                .pathParam("id", tempCardId)
                .delete("/cards/{id}");
        // Enhanced validation
        int statusCode = response.getStatusCode();
        assertTrue(statusCode == 200 || statusCode == 204,
                "Status code should be 200 or 204");
        if (statusCode == 204) {
            String body = response.getBody().asString();
            assertTrue(body.isEmpty() || body.equals("null"),
                    "Body should be empty or null for 204");
        } else if (statusCode == 200) {
            try {
                Map<String, Object> json = response.jsonPath().getMap("$");
                assertTrue(json.containsKey("limits"), "Response should have 'limits' key");
                Map<String, Object> limits = (Map<String, Object>) json.get("limits");
                assertTrue(limits.isEmpty(), "Limits should be empty object");
            } catch (Exception e) {
                String body = response.getBody().asString();
                assertTrue(body.equals("") || body.equals("null") || body.equals("{}"),
                        "Unexpected response body format");
            }
        }
    }
    // 05 - NEGATIVE TESTS (TC22-TC23)
    @Test
    @Order(22)
    @DisplayName("TC22 - Missing Required Field (Board Creation)")
    public void tc22_missingRequiredField() {
        Response response = getBaseRequest()
                .post("/boards");

        response.then()
                .statusCode(400)
                .log().ifError();
    }

    @Test
    @Order(23)
    @DisplayName("TC23 - Delete Board")
    public void tc23_deleteBoard() {
        Response response = getBaseRequest()
                .pathParam("id", boardId)
                .delete("/boards/{id}");

        response.then()
                .statusCode(200)
                .log().ifError();
        boardId = null; // Prevent cleanup from trying to delete again
    }
}