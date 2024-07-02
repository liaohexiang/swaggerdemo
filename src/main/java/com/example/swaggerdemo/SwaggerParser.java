package com.example.swaggerdemo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map.Entry;

public class SwaggerParser {

    public static void main(String[] args) throws IOException {
       /* if (args.length != 1) {
            System.out.println("Usage: java SwaggerSplitter <swagger-file>");
            System.exit(1);
        }*/

        //String inputFilePath = args[0];
        ObjectMapper mapper = new ObjectMapper();

        // 读取原始Swagger文档
        JsonNode swaggerDoc = mapper.readTree(new File("D:\\projects\\swaggerdemo\\src\\main\\resources\\static\\swagger2.json"));

        // 提取公共部分
        ObjectNode commonInfo = mapper.createObjectNode();
        commonInfo.set("swagger", swaggerDoc.get("swagger"));
        commonInfo.set("info", swaggerDoc.get("info"));
        commonInfo.set("host", swaggerDoc.get("host"));
        commonInfo.set("basePath", swaggerDoc.get("basePath"));
        commonInfo.set("schemes", swaggerDoc.get("schemes"));
        commonInfo.set("consumes", swaggerDoc.get("consumes"));
        commonInfo.set("produces", swaggerDoc.get("produces"));

        // 遍历所有路径
        JsonNode pathsNode = swaggerDoc.get("paths");
        Iterator<Entry<String, JsonNode>> paths = pathsNode.fields();
        while (paths.hasNext()) {
            Entry<String, JsonNode> pathEntry = paths.next();
            String path = pathEntry.getKey();
            JsonNode pathNode = pathEntry.getValue();

            // 创建新的Swagger文档
            ObjectNode newDoc = commonInfo.deepCopy();
            newDoc.set("paths", mapper.createObjectNode().set(path, pathNode));

            // 提取相关的definitions
            ObjectNode definitionsNode = mapper.createObjectNode();
            extractDefinitions(pathNode, swaggerDoc.get("definitions"), definitionsNode);
            newDoc.set("definitions", definitionsNode);

            // 保存新的Swagger文档
            String outputFileName = path.replaceAll("[^a-zA-Z0-9]", "_") + "_api.json";
            mapper.writerWithDefaultPrettyPrinter().writeValue(new File("D:\\projects\\swaggerdemo\\src\\main\\java\\com\\example\\swaggerdemo\\splitfiles\\"+outputFileName), newDoc);
        }
    }

    private static void extractDefinitions(JsonNode pathNode, JsonNode allDefinitions, ObjectNode relevantDefinitions) {
        if (pathNode.isObject()) {
            Iterator<Entry<String, JsonNode>> fields = pathNode.fields();
            while (fields.hasNext()) {
                Entry<String, JsonNode> field = fields.next();
                extractDefinitions(field.getValue(), allDefinitions, relevantDefinitions);
            }
        } else if (pathNode.isArray()) {
            for (JsonNode item : pathNode) {
                extractDefinitions(item, allDefinitions, relevantDefinitions);
            }
        } else if (pathNode.isTextual()) {
            String text = pathNode.asText();
            if (text.startsWith("#/definitions/")) {
                String definitionKey = text.substring("#/definitions/".length());
                if (allDefinitions.has(definitionKey)) {
                    relevantDefinitions.set(definitionKey, allDefinitions.get(definitionKey));
                }
            }
        }
    }
}

