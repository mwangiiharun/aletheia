# Aletheia Wiki

Welcome to the **Aletheia** wiki! This wiki contains detailed documentation, guides, and examples for using Aletheia in your projects.

## 📚 Documentation Pages

### Getting Started
- [Installation](Installation.md) - How to install and add Aletheia to your project
- [Quick Start](Quick-Start.md) - Get up and running in minutes
- [Configuration](Configuration.md) - Configure providers and settings

### Core Concepts
- [Providers](Providers.md) - Understanding secret providers (ENV, FILE, AWS, GCP, VAULT)
- [Provider Chain](Provider-Chain.md) - How the provider fallback chain works
- [Secret Injection](Secret-Injection.md) - Using `@Secret` annotations for automatic injection
- [Error Handling](Error-Handling.md) - Understanding exceptions and error handling

### Framework Integration
- [Spring Boot Integration](Spring-Boot-Integration.md) - Using Aletheia with Spring Boot
- [Quarkus Integration](Quarkus-Integration.md) - Using Aletheia with Quarkus
- [Plain Java](Plain-Java.md) - Using Aletheia without frameworks

### Advanced Topics
- [Custom Providers](Custom-Providers.md) - Creating custom secret providers
- [Caching](Caching.md) - Understanding TTL-based caching
- [Testing](Testing.md) - Testing with Aletheia
- [Best Practices](Best-Practices.md) - Recommended patterns and practices

### Provider-Specific Guides
- [AWS Secrets Manager](AWS-Secrets-Manager.md) - Detailed AWS provider guide
- [GCP Secret Manager](GCP-Secret-Manager.md) - Detailed GCP provider guide
- [HashiCorp Vault](HashiCorp-Vault.md) - Detailed Vault provider guide
- [File Provider](File-Provider.md) - Using JSON file-based secrets
- [Environment Variables](Environment-Variables.md) - Using environment variables

### Troubleshooting
- [Common Issues](Common-Issues.md) - Solutions to common problems
- [Debugging](Debugging.md) - Tips for debugging secret resolution
- [Migration Guide](Migration-Guide.md) - Migrating from older versions

## 🔗 Quick Links

- **GitHub Repository**: [mwangiiharun/aletheia](https://github.com/mwangiiharun/aletheia)
- **Maven Central**: [Search for io.github.mwangiiharun:aletheia](https://search.maven.org/search?q=g:io.github.mwangiiharun%20AND%20a:aletheia)
- **Issues**: [Report Issues](https://github.com/mwangiiharun/aletheia/issues)

## 💡 What is Aletheia?

**Aletheia** (Greek: "truth") is a lightweight, framework-agnostic secret management library for Java 17+ that simplifies secure configuration management across different environments.

### Key Features

✨ **Multi-Provider Support** - Fetch secrets from Environment Variables, Files, AWS Secrets Manager, Google Cloud Secret Manager, or HashiCorp Vault

🔄 **Automatic Fallback** - Configure a chain of providers with automatic fallback when secrets aren't found

💉 **Annotation-Based Injection** - Use `@Secret` annotations to automatically inject secrets into your classes

🏗️ **Framework Agnostic** - Works standalone or integrates seamlessly with Spring Boot and Quarkus

🛡️ **Thread-Safe** - Built with thread-safety in mind for concurrent environments

⚡ **Caching** - Built-in TTL-based caching to reduce provider calls

🔒 **Type-Safe** - Leverage Java's type system for better security

---

**Need help?** Check out the [Common Issues](Common-Issues.md) page or open an issue on GitHub.

