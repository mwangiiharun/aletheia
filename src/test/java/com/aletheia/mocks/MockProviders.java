package com.aletheia.mocks;

import com.aletheia.SecretProvider;

/**
 * Mock provider implementations for testing layered secret resolution
 * without invoking any real cloud SDKs or network connections.
 */
public final class MockProviders {

    private MockProviders() {
        // utility holder
    }

    /** ✅ Mock Google Cloud provider */
    public static class MockGcpProvider implements SecretProvider {

        @Override
        public String getSecret(String key) {
            return switch (key) {
                case "GCP_ONLY" -> "from-gcp";
                // Simulate missing for common/fallback keys
                default -> null;
            };
        }

        @Override
        public boolean supports(String key) {
            return true;
        }

        @Override
        public void close() {
            // no-op
        }
    }

    /** ✅ Mock AWS provider */
    public static class MockAwsProvider implements SecretProvider {

        @Override
        public String getSecret(String key) {
            return switch (key) {
                case "AWS_ONLY" -> "from-aws";
                default -> null;
            };
        }

        @Override
        public boolean supports(String key) {
            return true;
        }

        @Override
        public void close() {
            // no-op
        }
    }

    /** ✅ Mock Vault provider */
    public static class MockVaultProvider implements SecretProvider {

        @Override
        public String getSecret(String key) {
            return switch (key) {
                case "VAULT_ONLY" -> "from-vault";
                // ✅ Return null for COMMON_KEY to test proper fallback to FileProvider
                default -> null;
            };
        }

        @Override
        public boolean supports(String key) {
            return true;
        }

        @Override
        public void close() {
            // no-op
        }
    }
}