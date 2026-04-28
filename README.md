# shopsphere-e-commerce-sprint

## Docker Compose

This setup builds all services and starts the infrastructure in one command.

```bash
docker compose up --build
```

Stop and remove containers:

```bash
docker compose down
```

Notes:
- If your config repository uses different DB names or credentials, update the service-level environment variables in `docker-compose.yml`.
- Razorpay keys can be provided via `RAZORPAY_KEY_ID` and `RAZORPAY_KEY_SECRET` environment variables.
