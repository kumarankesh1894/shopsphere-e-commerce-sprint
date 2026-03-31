# Config Server

This module runs Spring Cloud Config Server for ShopSphere.

## Required environment variables

- `CONFIG_GIT_USERNAME`
- `CONFIG_GIT_PAT`

## Run

```powershell
Set-Location "D:\capgemini-training\Backend Sprint\shopsphere-backend\configserver"
mvn spring-boot:run
```

## Verify

```powershell
Invoke-RestMethod "http://localhost:8888/adminservice/dev"
```

