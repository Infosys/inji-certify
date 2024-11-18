# Inji Stack Setup

This guide provides instructions for setting up and running Inji Stack.

## Prerequisites
- Docker and Docker Compose installed on your system
- Git (to clone the repository)
- Basic understanding of Docker and container operations
### Building inji-web-proxy
Before running the docker-compose, you need to build the inji-web-proxy image:

```bash
# Clone the repository
git clone https://github.com/mosip/inji-web.git -b develop
cd inji-web/inji-web-proxy

# Build the Docker image
docker build -t inji-web-proxy:local .
```

## Directory Structure Setup
Create the following directory structure before proceeding:

```
docker-compose-injistack/
├── data/
│   └── CERTIFY_PKCS12/(p12 file generated at runtime)
├── certs/
│   └── oidckeystore.p12 (to be obtained during onboarding of mimoto to esignet)
├── loader_path/
│   └── certify/ (plugin jar to be placed here)
├── config/ (default setup should work as is for csvplugin, any other config changes user can make as per their setup)
│   ├── certify-default.properties
│   ├── certify-mock-identity.properties
│   ├── mimoto-default.properties
│   ├── mimoto-issuers-config.json
│   ├── mimoto-trusted-verifiers.json
│   └── credential-template.html
├── nginx.conf
├── certify_init.sql
└── docker-compose.yml
```

## Mock Certify Plugin Setup
You have two options for the certify plugin:

### Option 1: Use Existing Mock Plugin
- Supported versions: 0.3.0 and above
- Download the snapshot JAR from:
  ```
  https://oss.sonatype.org/content/repositories/snapshots/io/mosip/certify/mock-certify-plugin/0.3.0-SNAPSHOT/
  ```
- Place the downloaded JAR in `loader_path/certify/`

### Option 2: Create Custom Plugin
You can create your own plugin by implementing the following interface and place the resultant jar in loader_path:
```java
public interface DataProviderPlugin {
    // Implement your custom logic here
}
```

## Configuration Setup



### 1. Certificate Setup
- Place your PKCS12 certificate file (obtained from esignet onboarding) in:
  ```
  certs/oidckeystore.p12
  ```

### 2. Configuration Files
Ensure all configuration files are properly updated in the config directory:
- certify-default.properties
- certify-mock-identity.properties
- mimoto-default.properties
- mimoto-issuers-config.json
- mimoto-trusted-verifiers.json
- credential-template.html

## Running the Application

### 1. Start the Services
```bash
docker-compose up -d
```

### 2. Verify Services
Check if all services are running:
```bash
docker-compose ps
```

## Service Endpoints
The following services will be available:
- Database (PostgreSQL): `localhost:5433`
- Certify Service: `localhost:8090`
- Nginx: `localhost:80`
- Mimoto Service: `localhost:8099`
- Inji Web Proxy: `localhost:3010`
- Inji Web: `localhost:3001`

## Using the Application

### Accessing the Web Interface
1. Open your browser and navigate to `http://localhost:3001`
2. You can:
    - Download credentials
    - View credential status
    - Manage your digital identity

## Troubleshooting

### Common Issues and Solutions
1. Container startup issues:
   ```bash
   docker-compose logs [service_name]
   ```

2. Database connection issues:
    - Verify PostgreSQL container is running
    - Check database credentials in configuration

3. Plugin loading issues:
    - Verify plugin JAR is in the correct directory
    - Check plugin version compatibility

### Health Checks
Monitor service health:
```bash
docker-compose ps
docker logs [container_name]
```

## Stopping the Application
To stop all services:
```bash
docker-compose down
```

To stop and remove all containers and volumes:
```bash
docker-compose down -v
```

## Security Considerations
- Keep your PKCS12 certificate secure
- Regularly update configurations and credentials
- Monitor service logs for security issues


## Additional Resources
- [Inji Documentation](https://docs.mosip.io/inji)