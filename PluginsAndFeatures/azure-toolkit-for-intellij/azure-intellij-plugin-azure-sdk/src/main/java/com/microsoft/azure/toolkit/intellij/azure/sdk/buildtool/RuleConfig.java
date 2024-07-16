package com.microsoft.azure.toolkit.intellij.azure.sdk.buildtool;

import java.util.Collections;
import java.util.List;

/**
 * This class contains configuration options for code style rules.
 * It contains the methods to check, the client name and the antipattern message
 */
class RuleConfig {
    private final List<String> methodsToCheck;
    private final List<String> clientsToCheck;
    private final List<String> servicesToCheck;
    private final String antiPatternMessage;
    private final String recommendationText;
    private final String recommendationLink;

    static final String AZURE_PACKAGE_NAME = "com.azure";

    static final RuleConfig EMPTY_RULE = new RuleConfig(Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), "", "", "");

    /**
     * Constructor for RuleConfig.
     *
     * @param methodsToCheck     List of methods to check.
     * @param clientsToCheck     List of clients to check.
     * @param servicesToCheck    List of services to check.
     * @param antiPatternMessage AntiPattern message.
     * @param recommendationText Recommendation text for the antipattern.
     * @param recommendationLink Recommendation link for the antipattern.
     */
    public RuleConfig(List<String> methodsToCheck, List<String> clientsToCheck, List<String> servicesToCheck, String antiPatternMessage, String recommendationText, String recommendationLink) {
        this.methodsToCheck = methodsToCheck;
        this.clientsToCheck = clientsToCheck;
        this.servicesToCheck = servicesToCheck;
        this.antiPatternMessage = antiPatternMessage;
        this.recommendationText = recommendationText;
        this.recommendationLink = recommendationLink;
    }

    // Getters

    /**
     * This method returns the list of methods to check
     *
     * @return List of methods to check
     */
    public List<String> getMethodsToCheck() {
        return methodsToCheck;
    }

    /**
     * This method returns the list of clients to check
     *
     * @return List of clients to check
     */
    public List<String> getClientsToCheck() {
        return clientsToCheck;
    }

    /**
     * This method returns the list of services to check
     *
     * @return List of services to check
     */
    public List<String> getServicesToCheck() {
        return servicesToCheck;
    }

    /**
     * This method returns the antipattern message
     *
     * @return Antipattern message
     */
    public String getAntiPatternMessage() {
        return antiPatternMessage;
    }

    /**
     * This method returns the recommendation text
     *
     * @return Recommendation text
     */
    public String getRecommendationText() {
        return recommendationText;
    }

    /**
     * This method returns the recommendation link
     *
     * @return Recommendation link
     */
    public String getRecommendationLink() {
        return recommendationLink;
    }
}
