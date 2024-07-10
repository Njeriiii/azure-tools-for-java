package com.microsoft.azure.toolkit.intellij.azure.sdk.buildtool;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.PsiType;
import java.io.FileNotFoundException;

import com.microsoft.applicationinsights.TelemetryClient;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * This class reads the instrumentation key from the applicationInsights.json file
 * and returns a TelemetryClient object with the instrumentation key set.
 * This object is used to send telemetry data to Application Insights.
 */
public class TelemetryClientProvider extends LocalInspectionTool {

    /**
     * This method is called by the IntelliJ platform to build a visitor for the inspection.
     * @param holder The ProblemsHolder object that holds the problems found in the code.
     * @param isOnTheFly A boolean that indicates if the inspection is running on the fly.
     * @return
     */
    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {

        // Reset the methodCounts map for each new visitor
        TelemetryClientProviderVisitor.methodCounts.clear();
        return new TelemetryClientProviderVisitor(holder, isOnTheFly);
    }

    /**
     * This class is a visitor that visits the method calls in the code and tracks the method calls.
     */
    public static class TelemetryClientProviderVisitor extends JavaElementVisitor {

        private final ProblemsHolder holder;
        private final boolean isOnTheFly;

        // Create a TelemetryClient object
        // not final because the test involves Injecting the mock telemetry client to telemetryClient
        // Package-private to allow access from tests in the same package
        static TelemetryClient telemetryClient = getTelemetryClient();
        // Create a map to store the method counts
        static Map<String, Map<String, Integer>> methodCounts = new HashMap<>();

        // Create a Project object
        private static Project project;

        // Create a logger object
        private static final Logger LOGGER= Logger.getLogger(TelemetryClientProvider.class.getName());


        /** This constructor is used to create a visitor for the inspection
         * It initializes the holder and isOnTheFly fields.
         * @param holder The ProblemsHolder object that holds the problems found in the code.
         * @param isOnTheFly A boolean that indicates if the inspection is running on the fly.
         */
        public TelemetryClientProviderVisitor(ProblemsHolder holder, boolean isOnTheFly) {
            this.holder = holder;
            this.isOnTheFly = isOnTheFly;

            // Initialize start telemetry service when project is null
            // This is to ensure that the telemetry service is started only once
            if (project == null) {
                startTelemetryService();
            }
            project = holder.getProject();
        }

        /**  will only track the methods that are being called in the code
         * This method is called when a method call expression is visited.
         * @param expression The method call expression that is visited.
         */
        @Override
        public void visitMethodCallExpression(@NotNull PsiMethodCallExpression expression) {
            super.visitMethodCallExpression(expression);

            PsiReferenceExpression methodExpression = expression.getMethodExpression();
            String methodName = methodExpression.getReferenceName();
            String clientName = getClientName(expression); // Method to get client name

            if (methodName == null) {
                return;
            }

            if (clientName != null) {
                synchronized (methodCounts) {

                    // Increment the count of the method call for the client
                    methodCounts
                            .computeIfAbsent(clientName, k -> new HashMap<>())
                            .put(methodName, methodCounts.get(clientName).getOrDefault(methodName, 0) + 1);
                }
            }
        }

        /**
         * This method extracts the client name from the method call expression.
         * It checks if the method call expression is from an Azure SDK client.
         * @param expression The method call expression.
         * @return The client name extracted from the method call expression.
         */
        private String getClientName(PsiMethodCallExpression expression) {
            // Logic to extract client name from the method call expression
            PsiExpression qualifier = expression.getMethodExpression().getQualifierExpression();
            if (qualifier != null) {
                PsiType type = qualifier.getType();
                if (type != null && type.getCanonicalText().startsWith("com.azure")) {

                    // This will be "SyncPoller<String, String>"
                    String presentableText = type.getPresentableText();

                    // Strip out the generic parameters
                    String baseTypeName = presentableText.replaceAll("<.*>", "");
                    return baseTypeName;
                }
            }
            return null;
        }

        /**
         * This method starts the telemetry service.
         * It creates a single-threaded ScheduledExecutorService and
         * schedules the telemetry data to be sent every 2 minutes.
         */
        static void startTelemetryService() {
            ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

            executorService.scheduleAtFixedRate(() ->
                    TelemetryClientProviderVisitor.sendTelemetryData(), 2, 3, TimeUnit.MINUTES);
        }

        /**
         * This method sends the telemetry data to Application Insights.
         * It sends the method counts as events.
         */
        static void sendTelemetryData() {

            // Outer loop: Iterate over each client entry in the methodCounts map
            for (Map.Entry<String, Map<String, Integer>> clientEntry : methodCounts.entrySet()) {
                String clientName = clientEntry.getKey();  // Extract the client name
                Map<String, Integer> methods = clientEntry.getValue();  // Extract the methods map for this client

                // Inner loop: Iterate over each method entry in the methods map
                for (Map.Entry<String, Integer> methodEntry : methods.entrySet()) {
                    String methodName = methodEntry.getKey();  // Extract the method name
                    int count = methodEntry.getValue();  // Extract the call count for this method

                    // Create custom dimensions map
                    Map<String, String> customDimensions = new HashMap<>();
                    customDimensions.put("clientName", clientName);
                    customDimensions.put("methodName", methodName);

                    /// Convert count to a double and create a properties map
                    Map<String, Double> properties = new HashMap<>();
                    properties.put("count", (double) count);

                    // Report as event
                    telemetryClient.trackEvent("azure_sdk_usage_frequency", customDimensions, properties);
                }
            }
            telemetryClient.flush();
        }

        /**
         * This method reads the instrumentation key from the applicationInsights.json file
         * and returns a TelemetryClient object with the instrumentation key set.
         * This object is used to send telemetry data to Application Insights.
         *
         * @return A TelemetryClient object with the instrumentation key set.
         */
        static TelemetryClient getTelemetryClient() {

            String configFilePath = "META-INF/applicationInsights.json";
            String instrumentationKeyJsonKey = "instrumentationKey";

            // Create a new TelemetryClient object
            TelemetryClient telemetry = new TelemetryClient();
            StringBuilder jsonBuilder = new StringBuilder();

            // Read the instrumentation key from the applicationInsights.json file
            try (InputStream inputStream = TelemetryClient.class.getClassLoader().getResourceAsStream(configFilePath);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                if (inputStream == null) {
                    LOGGER.log(Level.SEVERE, "Configuration file not found at path: " + configFilePath
                            + ". Please ensure the file exists and is accessible.", new FileNotFoundException());
                    return telemetry; // Return the telemetry client even if the config file is not found
                }

                // while loop to read the json file
                // this is more memory efficient than reading the entire file at once and holding it in memory
                String line;
                while ((line = reader.readLine()) != null) {
                    jsonBuilder.append(line).append("\n");
                }

                JSONObject jsonObject = new JSONObject(jsonBuilder.toString());
                String instrumentationKey = jsonObject.getString(instrumentationKeyJsonKey);
                telemetry.getContext().setInstrumentationKey(instrumentationKey);

            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Unexpected error while loading instrumentation key"
                        + ". Please investigate further.", e);
            }
            return telemetry;
        }
    }
}