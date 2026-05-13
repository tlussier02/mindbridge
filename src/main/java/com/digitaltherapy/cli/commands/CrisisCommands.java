package com.digitaltherapy.cli.commands;

import com.digitaltherapy.cli.ApiClient;
import com.digitaltherapy.cli.Command;
import com.digitaltherapy.cli.MenuHandler;
import com.digitaltherapy.dto.CopingStrategy;
import com.digitaltherapy.dto.CrisisHub;
import com.digitaltherapy.dto.SafetyPlanDto;
import org.springframework.core.ParameterizedTypeReference;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;
import java.util.function.Supplier;

public class CrisisCommands {

    // =========================================================================
    // Crisis Sub-Menu Command (entry point from main menu)
    // =========================================================================
    public static class CrisisMenuCommand implements Command {
        private final Scanner scanner;
        private final ApiClient apiClient;
        private final Supplier<UUID> currentUserIdSupplier;

        public CrisisMenuCommand(Scanner scanner,
                                 ApiClient apiClient,
                                 Supplier<UUID> currentUserIdSupplier) {
            this.scanner = scanner;
            this.apiClient = apiClient;
            this.currentUserIdSupplier = currentUserIdSupplier;
        }

        @Override
        public void execute() {
            if (currentUserIdSupplier.get() == null) {
                System.out.println("\nPlease log in first to access Crisis Support.");
                return;
            }

            List<Command> commands = new ArrayList<>();
            commands.add(new CopingStrategiesCommand(apiClient));
            commands.add(new EmergencyResourcesCommand(apiClient, currentUserIdSupplier));
            commands.add(new SafetyPlanCommand(apiClient, currentUserIdSupplier));
            commands.add(new BackCommand());

            MenuHandler menu = new MenuHandler("Crisis Support", scanner, commands);
            menu.handleInput();
        }

        @Override
        public String getName() {
            return "Crisis Support";
        }

        @Override
        public String getDescription() {
            return "Access crisis resources and coping strategies";
        }
    }

    // =========================================================================
    // Coping Strategies Command
    // =========================================================================
    public static class CopingStrategiesCommand implements Command {
        private final ApiClient apiClient;

        public CopingStrategiesCommand(ApiClient apiClient) {
            this.apiClient = apiClient;
        }

        @Override
        public void execute() {
            try {
                List<CopingStrategy> strategies = apiClient.getPublic("/crisis/coping-strategies",
                    new ParameterizedTypeReference<List<CopingStrategy>>() {});

                if (strategies == null || strategies.isEmpty()) {
                    System.out.println("\nNo coping strategies available at this time.");
                    return;
                }

                System.out.println("\n========================================");
                System.out.println("  Coping Strategies");
                System.out.println("========================================");

                for (CopingStrategy strategy : strategies) {
                    System.out.printf("\n  %s [%s]%n", strategy.getName(), strategy.getCategory());
                    System.out.printf("  %s%n", strategy.getDescription());
                    System.out.printf("  Estimated Time: %d minutes%n", strategy.getEstimatedMinutes());

                    if (strategy.getSteps() != null && !strategy.getSteps().isEmpty()) {
                        System.out.println("  Steps:");
                        int stepNum = 1;
                        for (String step : strategy.getSteps()) {
                            System.out.printf("    %d. %s%n", stepNum++, step);
                        }
                    }
                    System.out.println("  ----------------------------------------");
                }
            } catch (Exception e) {
                System.out.println("Failed to load coping strategies: " + e.getMessage());
            }
        }

        @Override
        public String getName() {
            return "Coping Strategies";
        }

        @Override
        public String getDescription() {
            return "View available coping strategies";
        }
    }

    // =========================================================================
    // Emergency Resources Command
    // =========================================================================
    public static class EmergencyResourcesCommand implements Command {
        private final ApiClient apiClient;
        private final Supplier<UUID> currentUserIdSupplier;

        public EmergencyResourcesCommand(ApiClient apiClient,
                                         Supplier<UUID> currentUserIdSupplier) {
            this.apiClient = apiClient;
            this.currentUserIdSupplier = currentUserIdSupplier;
        }

        @Override
        public void execute() {
            try {
                CrisisHub hub = apiClient.getPublic("/crisis", CrisisHub.class);

                if (hub == null) {
                    System.out.println("\nUnable to load crisis resources.");
                    return;
                }

                System.out.println("\n========================================");
                System.out.println("  EMERGENCY RESOURCES");
                System.out.println("========================================");

                if (hub.getMessage() != null) {
                    System.out.println("  " + hub.getMessage());
                    System.out.println();
                }

                if (hub.getEmergencyResources() != null && !hub.getEmergencyResources().isEmpty()) {
                    System.out.println("  Emergency Contacts:");
                    System.out.println("  ----------------------------------------");
                    for (CrisisHub.EmergencyResource resource : hub.getEmergencyResources()) {
                        System.out.printf("  ** %s **%n", resource.getName());
                        System.out.printf("     Phone: %s%n", resource.getPhone());
                        System.out.printf("     %s%n", resource.getDescription());
                        System.out.printf("     Available 24/7: %s%n", resource.isAvailable24x7() ? "Yes" : "No");
                        System.out.println();
                    }
                }

                if (hub.getCopingStrategies() != null && !hub.getCopingStrategies().isEmpty()) {
                    System.out.println("  Quick Coping Strategies:");
                    for (String strategy : hub.getCopingStrategies()) {
                        System.out.println("    - " + strategy);
                    }
                    System.out.println();
                }

                if (hub.getSafetyPlanSummary() != null) {
                    System.out.println("  Safety Plan Summary:");
                    System.out.println("  " + hub.getSafetyPlanSummary());
                }
                System.out.println("========================================");
            } catch (Exception e) {
                System.out.println("Failed to load emergency resources: " + e.getMessage());
            }
        }

        @Override
        public String getName() {
            return "Emergency Resources";
        }

        @Override
        public String getDescription() {
            return "View emergency contacts and resources";
        }
    }

    // =========================================================================
    // Safety Plan Command
    // =========================================================================
    public static class SafetyPlanCommand implements Command {
        private final ApiClient apiClient;
        private final Supplier<UUID> currentUserIdSupplier;

        public SafetyPlanCommand(ApiClient apiClient,
                                 Supplier<UUID> currentUserIdSupplier) {
            this.apiClient = apiClient;
            this.currentUserIdSupplier = currentUserIdSupplier;
        }

        @Override
        public void execute() {
            try {
                SafetyPlanDto plan = apiClient.getPublic("/crisis/safety-plan", SafetyPlanDto.class);

                if (plan == null) {
                    System.out.println("\nNo safety plan found. Consider creating one with your therapist.");
                    return;
                }

                System.out.println("\n========================================");
                System.out.println("  My Safety Plan");
                System.out.println("========================================");

                if (plan.getWarningSignals() != null && !plan.getWarningSignals().isEmpty()) {
                    System.out.println("\n  1. Warning Signals:");
                    for (String signal : plan.getWarningSignals()) {
                        System.out.println("     - " + signal);
                    }
                }

                if (plan.getCopingStrategies() != null && !plan.getCopingStrategies().isEmpty()) {
                    System.out.println("\n  2. Coping Strategies:");
                    for (String strategy : plan.getCopingStrategies()) {
                        System.out.println("     - " + strategy);
                    }
                }

                if (plan.getTrustedContacts() != null && !plan.getTrustedContacts().isEmpty()) {
                    System.out.println("\n  3. Trusted Contacts:");
                    for (SafetyPlanDto.TrustedContactDto contact : plan.getTrustedContacts()) {
                        System.out.printf("     - %s (%s): %s%n",
                                contact.getName(), contact.getRelationship(), contact.getPhone());
                    }
                }

                if (plan.getProfessionalContacts() != null && !plan.getProfessionalContacts().isEmpty()) {
                    System.out.println("\n  4. Professional Contacts:");
                    for (String contact : plan.getProfessionalContacts()) {
                        System.out.println("     - " + contact);
                    }
                }

                if (plan.getEnvironmentSafetySteps() != null && !plan.getEnvironmentSafetySteps().isEmpty()) {
                    System.out.println("\n  5. Environment Safety Steps:");
                    for (String step : plan.getEnvironmentSafetySteps()) {
                        System.out.println("     - " + step);
                    }
                }

                if (plan.getReasonForLiving() != null && !plan.getReasonForLiving().isEmpty()) {
                    System.out.println("\n  6. Reason for Living:");
                    System.out.println("     " + plan.getReasonForLiving());
                }

                System.out.println("\n========================================");
            } catch (Exception e) {
                System.out.println("Failed to load safety plan: " + e.getMessage());
            }
        }

        @Override
        public String getName() {
            return "My Safety Plan";
        }

        @Override
        public String getDescription() {
            return "View your personal safety plan";
        }
    }

    // =========================================================================
    // Back Command
    // =========================================================================
    private static class BackCommand implements Command {
        @Override
        public void execute() {
            // Do nothing - returns to main menu
        }

        @Override
        public String getName() {
            return "Back to Main Menu";
        }

        @Override
        public String getDescription() {
            return "Return to the main menu";
        }
    }
}
