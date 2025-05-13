package com.tappayments.automation.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.restassured.response.Response;
import org.testng.Assert;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.emptyOrNullString;

public class CommonAutomationUtils {

    public static String stringToJson(Object object){

        String payload = "";
        ObjectMapper mapper = new ObjectMapper();
        try {
            payload = mapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return payload;
    }

    public static void verifyCommonResponseSuccessValidation(Response response, int statusCode){

        verifyCommonResponseSuccessValidation(response, statusCode, Map.of());
    }

    public static void verifyCommonResponseSuccessValidation(Response response, int statusCode, Map<String, Object> additionalParamChecks){

        verifyStatusCode(response, statusCode);
        verifyExactMatch(response, Map.of("live_mode",false, "status", "INITIATED", "response.code", "101"));
        if(!additionalParamChecks.isEmpty())
            verifyExactMatch(response, additionalParamChecks);
        verifyNonEmpty(response, List.of("transaction.created"));
    }

    public static void verifyCommonResponseFailedValidation(Response response, int statusCode, Map<String, Object> parameterChecks){

        verifyStatusCode(response, statusCode);
        verifyExactMatch(response, parameterChecks);
    }

    public static void verifyCommonResponseFailedValidation(JsonNode jsonResponse, int responseCode, int expectedResponseCode, String code, String error){

        String errorCode = jsonResponse.at("/errors/0/code").asText();
        String errorType = jsonResponse.at("/errors/0/error").asText();

        Assert.assertEquals(responseCode, expectedResponseCode, "Response code doesn't match.");
        Assert.assertEquals(errorCode, code, "Error code doesn't match!");
        Assert.assertEquals(errorType, error, "Error type doesn't match!");
    }

    public static void verifyCommonResponseFailedDescriptionValidation(JsonNode jsonResponse, int responseCode, int expectedResponseCode, String code, String error){

        String errorCode = jsonResponse.at("/errors/0/code").asText();
        String errorType = jsonResponse.at("/errors/0/description").asText();

        Assert.assertEquals(responseCode, expectedResponseCode, "Response code doesn't match.");
        Assert.assertEquals(errorCode, code, "Error code doesn't match!");
        Assert.assertEquals(errorType, error, "Error description doesn't match!");
    }

    public static void verifyStatusCode(Response response, int statusCode){

        response.then().statusCode(statusCode);
    }

    public static void verifyExactMatch(Response response, Map<String, Object> mactches){

        mactches.forEach((path, expectedValue) -> {
            if (expectedValue instanceof Double)
                response.then() .body(path, equalTo(((Double) expectedValue).floatValue()));
            else if (expectedValue instanceof String)
                response.then().body(path, equalTo((String) expectedValue));
            else if (expectedValue instanceof Integer)
                response.then().body(path, equalTo((Integer) expectedValue));
            else if (expectedValue instanceof Boolean)
                response.then().body(path,  is(expectedValue));
            else
                throw new IllegalArgumentException("Unsupported type : " + expectedValue.getClass());
        });
    }

    public static void verifyNonEmpty(Response response, List<String> checks){

        checks.forEach((path) -> {
            response.then().body(path, not(emptyOrNullString()));
        });
    }

    public static void verifyNonAvailableKey(Response response, List<String> paths){

        paths.forEach((fullPath) -> {
            String parentPath = fullPath.contains(".") ? fullPath.substring(0, fullPath.lastIndexOf(".")) : "";
            String key = fullPath.contains(".") ? fullPath.substring(fullPath.lastIndexOf(".") + 1) : fullPath;

            if (!parentPath.isEmpty()) {
                // Check that the key is not present within the specific object
                response.then().body(parentPath, not(hasKey(key)));
            } else {
                // Check that the key is not present at the root level
                response.then().body("$", not(hasKey(key)));
            }
        });
    }

    public static JsonNode convertToJson(String json){

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = null;
        try {
            // Convert JSON string to JsonNode
            jsonNode = objectMapper.readTree(json);
        } catch (JsonProcessingException e) {
            // Handle parsing errors specifically
            System.err.println("Error processing JSON: " + e.getMessage());
        } catch (NullPointerException e) {
            // Handle cases where expected fields are missing in JSON
            System.err.println("Missing field in JSON: " + e.getMessage());
        } catch (Exception e) {
            // Catch any other unforeseen exceptions
            System.err.println("An unexpected error occurred: " + e.getMessage());
        }

        return jsonNode;
    }

    public static String modifyJson(String jsonPayload, String operation, String jsonKey, Object newValue) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode rootNode = mapper.readTree(jsonPayload);

            if (rootNode instanceof ObjectNode rootObjectNode) {
                switch (operation) {
                    case "MODIFY" -> applyModification(rootObjectNode, jsonKey, newValue);
                    case "DELETE" -> deleteKey(rootObjectNode, jsonKey);
                    default -> throw new IllegalArgumentException("Unsupported operation: " + operation);
                }
                return mapper.writeValueAsString(rootObjectNode);
            } else {
                throw new RuntimeException("Expected JSON root element to be an ObjectNode");
            }

        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private static void applyModification(ObjectNode objectNode, String jsonKey, Object newValue) {
        if (jsonKey.contains(".")) {
            String[] keyHierarchy = jsonKey.split("\\.");
            JsonNode targetNode = traverseToNode(objectNode, keyHierarchy);
            if (targetNode instanceof ObjectNode targetObjectNode) {
                assignValue(targetObjectNode, keyHierarchy[keyHierarchy.length - 1], newValue);
            } else if (targetNode instanceof ArrayNode targetArrayNode) {
                assignValue(targetArrayNode, keyHierarchy[keyHierarchy.length - 1], newValue);
            } else {
                throw new RuntimeException("Expected a JSON ObjectNode at " + keyHierarchy[keyHierarchy.length - 2]);
            }
        } else {
            assignValue(objectNode, jsonKey, newValue);
        }
    }

    private static void deleteKey(ObjectNode objectNode, String jsonKey) {
        if (jsonKey.contains(".")) {
            String[] keyHierarchy = jsonKey.split("\\.");
            JsonNode targetNode = traverseToNode(objectNode, keyHierarchy);
            if (targetNode instanceof ObjectNode targetObjectNode) {
                targetObjectNode.remove(keyHierarchy[keyHierarchy.length - 1]);
            } else if (targetNode instanceof ArrayNode targetArrayNode) {
                ObjectNode firstObjectNode = (ObjectNode) targetArrayNode.get(0);
                deleteKey(firstObjectNode, keyHierarchy[keyHierarchy.length - 1]);
            } else {
                throw new RuntimeException("Expected a JSON ObjectNode at " + keyHierarchy[keyHierarchy.length - 2]);
            }
        } else {
            objectNode.remove(jsonKey);
        }
    }

    private static JsonNode traverseToNode(ObjectNode objectNode, String[] keyHierarchy) {
        JsonNode currentNode = objectNode;
        for (int i = 0; i < keyHierarchy.length - 1; i++) {
            if (currentNode.isArray()) {
                currentNode = currentNode.get(0);
            }
            currentNode = currentNode.path(keyHierarchy[i]);
            if (!currentNode.isObject() && !currentNode.isArray()) {
                throw new RuntimeException("Expected a JSON ObjectNode at " + keyHierarchy[i]);
            }
        }
        return currentNode;
    }

    private static void assignValue(JsonNode node, String jsonKey, Object newValue) {
        if (node.isObject()) {
            ObjectNode targetObjectNode = (ObjectNode) node;
            if (newValue instanceof Double) {
                targetObjectNode.put(jsonKey, (Double) newValue);
            } else if (newValue instanceof Integer) {
                targetObjectNode.put(jsonKey, (Integer) newValue);
            } else if (newValue instanceof String) {
                targetObjectNode.put(jsonKey, (String) newValue);
            } else if (newValue instanceof Boolean) {
                targetObjectNode.put(jsonKey, (Boolean) newValue);
            } else {
                throw new IllegalArgumentException("Unsupported value type: " + newValue.getClass());
            }
        } else if (node.isArray()) {
            ArrayNode targetArrayNode = (ArrayNode) node;
            for (JsonNode arrayElement : targetArrayNode) {
                if (arrayElement.isObject()) {
                    assignValue(arrayElement, jsonKey, newValue);
                }
            }
        } else {
            throw new IllegalArgumentException("Unsupported node type: " + node.getClass());
        }
    }
}
