package org.example.responsehandler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.jsonSchema.JsonSchemaGenerator;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.report.ProcessingMessage;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.sun.codemodel.JCodeModel;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FileUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.jsonschema2pojo.*;
import org.jsonschema2pojo.rules.RuleFactory;
import org.testng.Assert;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Set;

@Log4j2
public class SchemaValidator extends AbstractResponseValidator {

    @Override
    public void validateResponse(String actualResponseBody, String expectedResponseBody,
                                 int actualResponseCode, int expectedResponseCode) {
        validateHttpCode(actualResponseCode, expectedResponseCode);

        try {
            String expectedResponseSchema = prepareExpectedSchema(expectedResponseBody);
            JsonNode actualSchema = JsonLoader.fromString(actualResponseBody);
            JsonNode expectedSchema = JsonLoader.fromString(expectedResponseSchema);

            JsonSchemaFactory factory = JsonSchemaFactory.byDefault();
            JsonSchema schema = factory.getJsonSchema(expectedSchema);
            ProcessingReport report = schema.validateUnchecked(actualSchema, true);

            if (!report.isSuccess()) {
                log.error("Expected response schema: {}", expectedResponseSchema);
                String message = buildValidationErrorMessage(report);
                Assert.fail("Expected response schema didn't contain actual response schema: " + message);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String prepareExpectedSchema(String expectedResponseBody) {
        generatePOJOForJSON(expectedResponseBody);
        compilePOJOs();
        String expectedResponseSchema = getJSONSchema();
        return addRequiredAttribute(expectedResponseSchema);
    }

    private String buildValidationErrorMessage(ProcessingReport report) {
        StringBuilder message = new StringBuilder();
        for (ProcessingMessage processingMessage : report) {
            message.append(String.format("Schema validation failure report: %n%s%n", processingMessage.getMessage()));

            appendFieldMessage(message, processingMessage, "instance.pointer", "Error at field: ");
            appendFieldMessage(message, processingMessage, "expected", "Expected value was: ");
            appendFieldMessage(message, processingMessage, "found", "Actual value was: ");
        }
        return message.toString();
    }

    private void appendFieldMessage(StringBuilder message, ProcessingMessage processingMessage, String fieldPath, String prefix) {
        JsonNode field = processingMessage.asJson().get(fieldPath);
        if (field != null && !field.toString().trim().isEmpty()) {
            message.append(String.format("%s%s%n", prefix, field));
        }
    }

    /**
     * This method adds the validation constraints to the raw JSON string generated by the getJSONSchema method.
     *
     * @param inputSchema the raw JSON schema as a string
     * @return the modified JSON schema as a string
     */
    @SuppressWarnings("unchecked")
    private String addRequiredAttribute(String inputSchema) {
        try {
            JSONObject rootObject = (JSONObject) new JSONParser().parse(inputSchema);
            addObjectsToMap(rootObject);
            rootObject.put("$schema", "http://json-schema.org/draft-04/schema#");
            return rootObject.toJSONString();
        } catch (ParseException e) {
            throw new RuntimeException("Failed to parse input schema", e);
        }
    }

    @SuppressWarnings("unchecked")
    private void addObjectsToMap(JSONObject rootObj) throws ParseException {
        if (!rootObj.containsKey("properties")) {
            return; // Early return if no properties exist
        }

        JSONObject properties = (JSONObject) rootObj.get("properties");
        Set<String> childProperties = properties.keySet();
        JSONArray requiredProperties = new JSONArray();

        for (String property : childProperties) {
            requiredProperties.add(property);
            JSONObject childNode = (JSONObject) properties.get(property);

            // Recursively process the child node
            addObjectsToMap(childNode);

            // Update the properties with the modified child node
            properties.put(property, childNode);
        }

        rootObj.put("properties", properties);
        rootObj.put("required", requiredProperties);
    }

    /**
     * Generates the raw JSON schema, which will be enriched with validation constraints.
     *
     * @return Raw string of JSON Schema. This string does not have any strict JSON field validation checks.
     */
    private String getJSONSchema() {
        ObjectMapper mapper = new ObjectMapper();
        com.fasterxml.jackson.module.jsonSchema.JsonSchema finalSchema = getFinalschema(mapper);

        try {
            return mapper.writeValueAsString(finalSchema);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to convert final schema to JSON string", e);
        }
    }

    private com.fasterxml.jackson.module.jsonSchema.JsonSchema getFinalschema(ObjectMapper mapper) {
        JsonSchemaGenerator schemaGen = new JsonSchemaGenerator(mapper);
        Class<?> clazz = loadExpectedResponseClass();

        try {
            return schemaGen.generateSchema(clazz);
        } catch (JsonMappingException | SecurityException e) {
            throw new RuntimeException("Failed to generate JSON schema", e);
        }
    }

    private Class<?> loadExpectedResponseClass() {
        URL[] urls = getClassUrls();
        try {
            ClassLoader cl = new URLClassLoader(urls);
            return cl.loadClass("com.schema.pojo.ExpectedResponsePojo");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("ExpectedResponsePojo class not found", e);
        }
    }

    private URL[] getClassUrls() {
        String currentPath = System.getProperty("user.dir");
        File file = new File(currentPath, "SchemaPojo");

        try {
            return new URL[]{file.toURI().toURL()};
        } catch (MalformedURLException e) {
            throw new RuntimeException("Failed to convert file to URL", e);
        }
    }

    /**
     * Compiles the POJOs created so that they can be loaded dynamically at runtime for reverse schema generation.
     */
    private void compilePOJOs() {
        String currentPath = System.getProperty("user.dir");
        File pojoFolder = new File(currentPath, "SchemaPojo/com/schema/pojo");

        File[] sourceFiles = pojoFolder.listFiles();
        if (sourceFiles == null || sourceFiles.length == 0) {
            throw new RuntimeException("No source files found in " + pojoFolder.getAbsolutePath());
        }

        String[] sourceFilePaths = new String[sourceFiles.length];
        for (int i = 0; i < sourceFiles.length; i++) {
            sourceFilePaths[i] = sourceFiles[i].getPath();
        }
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new RuntimeException("Java compiler not available. Ensure that the JDK is used to run this application.");
        }

        int compilationResult = compiler.run(null, null, null, sourceFilePaths);
        if (compilationResult != 0) {
            throw new RuntimeException("Compilation failed with error code: " + compilationResult);
        }
    }

    /**
     * Generates the POJOs for the expected response body passed in the data provider.
     * These POJOs will be further used for schema validation.
     *
     * @param inputString the JSON string for which POJOs are to be generated
     */
    private void generatePOJOForJSON(String inputString) {
        String packageName = "com.schema.pojo";
        File inputJson = new File("expectedResponsePojo.json");

        // Write the input JSON string to a file
        writeJsonToFile(inputJson, inputString);

        // Prepare the output directory for the generated POJOs
        File outputPojoDirectory = new File("SchemaPojo");
        deleteExistingPojoDirectory(outputPojoDirectory);

        // Create the output directory
        if (!outputPojoDirectory.mkdirs()) {
            throw new RuntimeException("Failed to create POJO output directory: " + outputPojoDirectory.getAbsolutePath());
        }

        // Generate the POJOs from the JSON file
        generatePOJOsFromJson(inputJson, outputPojoDirectory, packageName);
    }

    /**
     * Writes the given JSON string to a specified file.
     *
     * @param file the file to write to
     * @param jsonString the JSON string to write
     */
    private void writeJsonToFile(File file, String jsonString) {
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(jsonString);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write JSON to file: " + file.getAbsolutePath(), e);
        }
    }

    /**
     * Deletes the existing POJO directory if it exists.
     *
     * @param directory the directory to delete
     */
    private void deleteExistingPojoDirectory(File directory) {
        if (directory.exists()) {
            try {
                FileUtils.deleteDirectory(directory);
            } catch (IOException e) {
                throw new RuntimeException("Failed to delete existing POJO directory: " + directory.getAbsolutePath(), e);
            }
        }
    }

    /**
     * Generates POJOs from the specified JSON file.
     *
     * @param inputJson the JSON file
     * @param outputDirectory the directory to output the generated POJOs
     * @param packageName the package name for the generated POJOs
     */
    private void generatePOJOsFromJson(File inputJson, File outputDirectory, String packageName) {
        try {
            JSONtoPOJO(inputJson.toURI().toURL(), outputDirectory, packageName, inputJson.getName().replace(".json", ""));
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate POJOs from JSON: " + inputJson.getAbsolutePath(), e);
        }
    }

    private void JSONtoPOJO(URL inputJson, File outputPojoDirectory, String packageName, String className) {
        JCodeModel codeModel = new JCodeModel();

        // Configure the schema mapper
        SchemaMapper mapper = getSchemaMapper();

        // Generate the POJOs
        mapper.generate(codeModel, className, packageName, inputJson);

        // Build the generated code to the output directory
        try {
            codeModel.build(outputPojoDirectory);
        } catch (IOException e) {
            throw new RuntimeException("Failed to build POJOs in directory: " + outputPojoDirectory.getAbsolutePath(), e);
        }
    }

    private static SchemaMapper getSchemaMapper() {
        GenerationConfig config = new DefaultGenerationConfig() {
            @Override
            public boolean isGenerateBuilders() {
                return false; // Disable builder generation
            }

            @Override
            public SourceType getSourceType() {
                return SourceType.JSON; // Specify the source type as JSON
            }
        };

        // Create the schema mapper with the configured settings
        return new SchemaMapper(
                new RuleFactory(config, new Jackson2Annotator(config), new SchemaStore()),
                new SchemaGenerator()
        );
    }


}

